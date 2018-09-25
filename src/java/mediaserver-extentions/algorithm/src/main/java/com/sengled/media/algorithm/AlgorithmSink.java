package com.sengled.media.algorithm;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.FramePacket.Flags;
import com.sengled.media.MediaSource;
import com.sengled.media.SingletonMediaSink;
import com.sengled.media.StreamContext;
import com.sengled.media.algorithm.config.AlgorithmConfig;
import com.sengled.media.clock.Rational;
import com.sengled.media.clock.SystemClock;
import com.sengled.media.plugin.FrameStorer;
import com.sengled.media.plugin.config.storage.StorageConfig;
import com.sengled.media.server.MutableFramePacket;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.SystemPropertyUtil;

/**
 * Created by las on 2017/3/16.
 */
public class AlgorithmSink extends SingletonMediaSink {

    private static final int ALGORITHM_START_DELAY = 30 * 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(AlgorithmSink.class);
    private static final int MDFLAG_TIMEOUT;
    private static final int KEEP_ALIVE_TIMEOUT;
    private static final int MAX_ALIVE_TIMEOUT;

    static {
        MDFLAG_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(SystemPropertyUtil.getInt("algorithm.mdflag.timeout", 4));
        KEEP_ALIVE_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(SystemPropertyUtil.getInt("algorithm.keepalive.timeout", 3 * 60));
        MAX_ALIVE_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(SystemPropertyUtil.getInt("algorithm.maxAlive.timeout", 10 * 60));

        LOGGER.info("-Dalgorithm.mdflag.timeout={} (seconds)", MDFLAG_TIMEOUT / 1000);
        LOGGER.info("-Dalgorithm.keepalive.timeout={} (seconds)", KEEP_ALIVE_TIMEOUT / 1000);
        LOGGER.info("-Dalgorithm.maxAlive.timeout={} (seconds)", MAX_ALIVE_TIMEOUT / 1000);
    }

    private final long startAt = SystemClock.currentTimeMillis();
    private final String token;
    private final FrameStorer storer;
    private final int videoStreamIndex;

    private final AlgorithmSinkConfig config;
    /**
     * snap2 中新增了 PIR 元件
     */
    private boolean hasPir = false;
    private long lastPirTime = -1;
    private long lastMdTime = -1;

    /**
     * 为了节省带宽，有些固件 GoP 被设置成 50，其中第 25 帧是可以依赖 I 帧独立解码的
     */
    private int frameIndex;

    private MutableFramePacket lastKeyFrame;
    private AlgorithmStream stream;

    public AlgorithmSink(MediaSource resource, FrameStorer storer, AlgorithmSinkConfig config) {
        super(resource);
        this.token = resource.getToken();
        this.storer = storer;
        this.config = config;

        int videoStreamIndex = 0;
        StreamContext<?>[] streams = resource.getStreamContexts();
        for (int i = 0; i < streams.length; i++) {
            if (null != streams[i] && streams[i].getCodec().isVideo()) {
                videoStreamIndex = i;
            }
        }
        this.videoStreamIndex = videoStreamIndex;
    }

    @Override
    protected void onClosed() {
        algorithmClose();
        releaseLastKeyFrame();
        LOGGER.info("[{}] algorithm closed", token);
    }


    @Override
    protected void finalize() throws Throwable {
        releaseLastKeyFrame();
    }

    private void releaseLastKeyFrame() {
        MutableFramePacket keyFrame = lastKeyFrame;
        lastKeyFrame = null;

        ReferenceCountUtil.release(keyFrame);
    }


    @Override
    public void onFrame(MutableFramePacket frame) throws IOException {
        try {
            if (!isAcceptable(frame)) {
                return;
            }

            if (isTimeout() || frame.isDiscontinued()) {
                algorithmClose(); // 超时关闭算法上下文
            }

            // 视频帧计数器 +1
            frameIndex = frame.isKeyFrame() ? (0) : (frameIndex + 1);

            // snap2 有 pir 元件， snap1 没有
            this.hasPir = hasPir || frame.hasFlag(Flags.HAS_PIR);
            if (frame.hasFlag(Flags.PIR_FRAME)) {
                this.lastPirTime = frame.getTime(Rational.MILLISECONDS);
            }
            if (frame.hasFlag(Flags.MD_FRAME)) {
                this.lastMdTime = frame.getTime(Rational.MILLISECONDS);
            }


            if (frame.isImcomplete()) {
                return; // 1, 不完整的包，不处理
            }

            // 2, 当前帧是关键字，需要缓存起来，等待确认是否有 MD 标记
            if (frame.isKeyFrame()) {
                // 在优化后的 snap 设备中，前后两个 I  帧之间，可能会出现一个特殊的 P 帧， 他可以相对于前面的 I 帧解码。
                // 引入 P 帧以后，整个视频流的 I 帧数量就减少了
                flushLastKeyFrameIfHasMD();

                // 更新新的 key 帧
                lastKeyFrame = frame.retain();

                // 日志
                if (null != stream && LOGGER.isDebugEnabled()) {
                    long keepAliveTimeout = SystemClock.currentTimeMillis() - stream.getLastModified();
                    LOGGER.debug("[{}] remains {}s timeout", token, TimeUnit.MILLISECONDS.toSeconds(KEEP_ALIVE_TIMEOUT - keepAliveTimeout));
                }
            }

            // 可以参考关键帧解码的 P 帧
            else if (null != lastKeyFrame && frameIndex % config.getFps() == 0) {
                lastKeyFrame = MutableFramePacket.composite(lastKeyFrame, frame.retain());
                LOGGER.debug("[{}] append {}th p frame, size = {}", token, frameIndex, lastKeyFrame.content().readableBytes());
            }
        } finally {
            frame.release();
        }
    }


    private boolean isAcceptable(MutableFramePacket frame) {
        if (SystemClock.currentTimeMillis() - startAt < ALGORITHM_START_DELAY) {
            return false; // 延时启动，避免频繁创建算法上下文导致算法服务崩溃
        }

        if (frame.getStreamIndex() != videoStreamIndex) {
            return false;
        }

        return true;
    }


    private boolean isTimeout() {
        if (null == stream) {
            return false;
        }

        // 为了负载均衡，每个 stream 的存活时间固定为 10min
        if (SystemClock.currentTimeMillis() - stream.getCreateTime() > MAX_ALIVE_TIMEOUT) {
            return true;
        }

        // 为了精度，每个 stream 超过 5min 没有收到新的数据时，自动关闭
        long keepAliveTimeout = SystemClock.currentTimeMillis() - stream.getLastModified();
        if (keepAliveTimeout > KEEP_ALIVE_TIMEOUT) {
            return true;
        }

        return false;
    }


    private boolean flushLastKeyFrameIfHasMD() {
        MutableFramePacket frame = lastKeyFrame;
        lastKeyFrame = null;
        try {
            if (null == frame) {
                return false; // 没有帧
            }

            if ((hasPir && config.isPirUsed()) && isFlagTimeout(frame, lastPirTime)) {
                LOGGER.debug("[{}] pir expired", token);
                return false; // pir md 的间隔太长了，无效
            }

            if (isFlagTimeout(frame, lastMdTime)) {
                LOGGER.debug("[{}] md expired", token);
                return false;  // md 的间隔太长了，无效
            }

            final int detectionInternal = config.getDetectionInternal();
            final long frameTime = frame.getTime(Rational.MILLISECONDS);
            if (null != stream
                    && frameTime - stream.getLastModified() < detectionInternal) {
                LOGGER.debug("[{}] internal {}ms < {}ms, the frame ignored.", token, frameTime - stream.getLastModified(), detectionInternal);
                return false; // 对免费用户，需要控制检测的频率
            }

            if (null == stream) {
                stream = new AlgorithmStream(this, storer);
            }
            stream.send(frame.retain());
            return true;
        } finally {
            ReferenceCountUtil.release(frame);
        }
    }


    private final boolean isFlagTimeout(MutableFramePacket frame, long lastMdTime) {
        return lastMdTime < 1 || (frame.getTime(Rational.MILLISECONDS) - lastMdTime > MDFLAG_TIMEOUT);
    }

    private void algorithmClose() {
        AlgorithmStream old = stream;
        stream = null;

        IOUtils.closeQuietly(old);
    }


    public String getToken() {
        return token;
    }

    public AlgorithmSinkConfig getConfig() {
        return config;
    }

    public AlgorithmConfig getAlgorithmConfig() {
        return config.getAlgorithmConfig();
    }

    public StorageConfig getStorageConfig() {
        return config.getStorageConfig();
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
