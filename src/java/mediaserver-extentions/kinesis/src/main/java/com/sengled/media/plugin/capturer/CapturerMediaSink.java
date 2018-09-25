package com.sengled.media.plugin.capturer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.services.kinesis.producer.Attempt;
import com.amazonaws.services.kinesis.producer.UserRecordFailedException;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.sengled.media.MediaSource;
import com.sengled.media.SingletonMediaSink;
import com.sengled.media.plugin.FrameStorer;
import com.sengled.media.server.MutableFramePacket;
import io.netty.buffer.ByteBufUtil;

/**
 * Created by las on 2017/2/23.
 */
public class CapturerMediaSink extends SingletonMediaSink {

    private static Logger LOGGER = LoggerFactory.getLogger(CapturerMediaSink.class);

    private final FrameStorer storer;
    private final int interval;
    private final String token;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    private Long lastTime;


    public CapturerMediaSink(MediaSource resource, FrameStorer storer, int interval) {
        super(resource);
        this.storer = storer;
        this.token = resource.getToken();
        this.interval = interval;
        this.lastTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(interval - 5);
    }


    @Override
    public void onFrame(MutableFramePacket frame) {
        try {
            if (frame.isKeyFrame() && !frame.isImcomplete() && isWritable()) {
                lastTime = System.currentTimeMillis();
                ByteBuffer buffer = ByteBuffer.allocate(frame.content().readableBytes());
                frame.content().readBytes(buffer);
                buffer.flip();
                if (buffer.get(0) != 0 || buffer.get(1) != 0 || buffer.get(2) != 0 || buffer.get(3) != 1) {
                    LOGGER.error("[{}] frame is not start with 00 00 00 01", token);
                    LOGGER.error(ByteBufUtil.prettyHexDump(frame.content()));
                } else {
                    storer.save(token, buffer, getCallBack(buffer.remaining()));
                    LOGGER.debug("[{}] did capture", token);
                }
            }
        } finally {
            frame.release();
        }

    }

    private FutureCallback<UserRecordResult> getCallBack(int length) {
        return new FutureCallback<UserRecordResult>() {
            @Override
            public void onFailure(Throwable t) {
                if (t instanceof UserRecordFailedException) {
                    Attempt last = Iterables.getLast(
                            ((UserRecordFailedException) t).getResult().getAttempts());
                    LOGGER.error(String.format(
                            "Record failed to put - %s : %s",
                            last.getErrorCode(), last.getErrorMessage()));
                }
                LOGGER.error("Exception during put", t);
            }

            @Override
            public void onSuccess(UserRecordResult result) {
                LOGGER.debug("[{}] send {} bytes to shard [{}]", token, length, result.getShardId());
            }
        };
    }

    public boolean isWritable() {
//        DeviceConfig config = DeviceConfigManager.getConfig(token);
//        if (config == null
//                || config.getScreenshot() == null
//                || !config.getScreenshot().isEnable()
//                || config.getScreenshot().getIntervalSeconds() <= 0) {
//            return false;
//        }
//        if (config.getScreenshot().getIntervalSeconds() - interval != 0) {
//            interval = config.getScreenshot().getIntervalSeconds();
//        }
        if (lastTime == null) return true;

        Long itl = System.currentTimeMillis() - lastTime;
        return itl >= timeUnit.toMillis(interval);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CapturerMediaSink) {
            CapturerMediaSink sink = (CapturerMediaSink)obj;
            return this.token.equals(sink.token);
        }
        return super.equals(obj);
    }

    @Override
    protected void onClosed() throws IOException {
        LOGGER.info("[{}] capturer closed", token);
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{").append(getClass().getSimpleName());
        buf.append(", token=").append(token);
        buf.append("}");
        return buf.toString();
    }
}
