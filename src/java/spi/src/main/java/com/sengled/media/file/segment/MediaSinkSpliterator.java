package com.sengled.media.file.segment;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.MediaSource;
import com.sengled.media.SingletonMediaSink;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.MutableFramePacket;

/**
 * 把视频流切割成小片段
 *
 * @author chenxh
 */
public abstract class MediaSinkSpliterator extends SingletonMediaSink {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaSinkSpliterator.class);

    private Segment segment;

    public MediaSinkSpliterator(MediaSource source) {
        super(source);
    }

    protected abstract SplitStrategy strategy();

    @Override
    protected final void onClosed() throws IOException {
        try {
            onClosed0();
        } catch (IOException e) {
            LOGGER.error("Fail close segment", e);
        } finally {
            closeSegment();
        }
    }

    protected void onClosed0() throws IOException {}
    
    private void closeSegment() throws IOException {
        if (null != segment) {
            strategy().close(segment);
            segment = null;
        }
    }

    @Override
    public void onFrame(MutableFramePacket frame) throws IOException {
        try {
            SplitStrategy strategy = strategy();

            // 如果满足要求，则自动关闭
            if (null != segment) {
                segment.setNextFrameTime(frame.getTime(Rational.MILLISECONDS));
                if ((frame.isDiscontinued() && segment.getDuration() > 0) || strategy.isNeedClose(segment, frame.retain())) {
                    closeSegment();
                }
            }

            // 创建新的切片
            if (null == segment || segment.isClosed()) {
                segment = strategy.nextSegment(frame.content().alloc(), frame.getTime(Rational.MILLISECONDS));
            }

            // 文件追加
            segment.appendFrame(frame.retain());
        } finally {
            frame.release();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        closeSegment();
    }

}
