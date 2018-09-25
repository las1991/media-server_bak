package com.sengled.media.dispatcher;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.FramePacket;
import com.sengled.media.MediaDispatcher;
import com.sengled.media.MediaSink;
import com.sengled.media.MediaSinkGroup;
import com.sengled.media.MediaSource;
import com.sengled.media.StreamContext;
import com.sengled.media.dispatcher.strategy.MediaDispatchStrategy;
import com.sengled.media.event.EventType;
import com.sengled.media.server.MutableFramePacket;
import com.sengled.media.server.MutableFramePacketBuffer;
import com.sengled.media.server.StreamingMediaSink;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.internal.RecyclableArrayList;


/**
 * 把 rtsp 的流作分发
 *
 * @author chenxh
 */
public class DefaultMediaDispatcher implements Closeable, MediaDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMediaDispatcher.class);

    private final MediaSinkGroup sinks;

    private final MutableFramePacketBuffer buffer = new MutableFramePacketBuffer();
    private final MediaDispatchStrategy stratrgy;
    private final MediaSource source;

    public DefaultMediaDispatcher(MediaSource source, String token, StreamContext<?>[] streams) {
        this.sinks = new DefaultMediaSinkGroup();
        this.source = source;
        this.stratrgy = MediaDispatchStrategy.newInstance(token, streams);
    }

    @Override
    public void dispatch(FramePacket src) {
        RecyclableArrayList out = RecyclableArrayList.newInstance();
        try {
            stratrgy.read(src, out);

            while(!out.isEmpty()) {
                final MutableFramePacket frame = fastCopy((MutableFramePacket) out.remove(0));
                try {
                    // 如果视频分辨率发生变化，则清空缓存
                    if (frame.isDiscontinued()) {
                        LOGGER.info("[{}] discontinued", source.getToken());
                        discontinued();
                    }

                    // 分发数据
                    if (!sinks.isEmpty()) {
                        int dispatched = sinks.dispatch(frame.duplicate().retain());
                        if (dispatched > 0) {
                            LOGGER.debug("dispatch {} sinks with {}", dispatched, frame);
                        }
                    }

                    // 加入缓存
                    buffer.add(frame.retain());
                } finally {
                    frame.release();
                }
            }
        } finally {
            release(out);
        }
    }
    
    private MutableFramePacket fastCopy(MutableFramePacket src) {
        try {
            if (src.content()  instanceof CompositeByteBuf) {
                return src.copy();
            } else {
                return src.duplicate().retain();
            }
        } finally {
            src.release();
        }
    }


    private void release(RecyclableArrayList out) {
        try {
            for (int i = 0; i < out.size(); i++) {
                MutableFramePacket frame = (MutableFramePacket) out.get(i);

                // 调试日志
                if (!out.isEmpty() && LOGGER.isTraceEnabled()) {
                    LOGGER.trace("[{}] {}/{}, {}", source.getToken(), i, out.size(), frame);
                }

                frame.release();
            }
        } finally {
            out.recycle();
        }
    }

    private void discontinued() {
        for (MediaSink mediaSink : getMediaSinks()) {
            if (mediaSink instanceof StreamingMediaSink) {
                boolean removed = removeMediaSink(mediaSink);
                if (removed) {
                    mediaSink.close();
                }
            }
        }

        // 把 buffer 清空了
        buffer.clear();
        
        source.postEvent(EventType.DISCONTINUED);
    }


    /* (non-Javadoc)
     * @see com.sengled.media.MediaDispatcher#getNumMediaSinks()
     */
    @Override
    public int getNumSinks() {
        return sinks.size();
    }


    /* (non-Javadoc)
     * @see com.sengled.media.MediaDispatcher#getMediaSink(java.lang.Class)
     */
    @Override
    public <T extends MediaSink> T getMediaSink(Class<T> type) {
        List<T> sinks = getMediaSinks(type);

        return sinks.isEmpty() ? null : sinks.get(0);
    }

    @Override
    public <T extends MediaSink> List<T> getMediaSinks(Class<T> type) {
        List<MediaSink> sinks = getMediaSinks();

        List<T> typedSinks = new ArrayList<>();
        for (MediaSink sink : sinks) {
            if (null != sink && type.isInstance(sink)) {
                typedSinks.add((T) sink);
            }
        }

        return typedSinks;
    }


    @Override
    public List<MediaSink> getMediaSinks() {
        List<MediaSink> results = new ArrayList<>();
        results.addAll(this.sinks.getMediaSinks());
        return results;
    }

    /* (non-Javadoc)
     * @see com.sengled.media.MediaDispatcher#addMediaSink(com.sengled.media.MediaSink)
     */
    @Override
    public boolean addMediaSink(MediaSink newSink) {
        return this.setupNewMediaSinks(newSink) && sinks.add(newSink);
    }

    private boolean setupNewMediaSinks(MediaSink mediaSink) {
        try {
            if (null != mediaSink) {
                // SNAP 产品，GoP 为 2s
                List<MutableFramePacket> frames = buffer.getLastGoP(2 * 1000);
                mediaSink.setup(frames);
                
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Fail setup {}", mediaSink, e);
        }

        return false;
    }

    /* (non-Javadoc)
     * @see com.sengled.media.MediaDispatcher#removeMediaSink(com.sengled.media.MediaSink)
     */
    @Override
    public boolean removeMediaSink(MediaSink sink) {
        return sinks.remove(sink);
    }

    /* (non-Javadoc)
     * @see com.sengled.media.MediaDispatcher#close()
     */
    @Override
    public final void close() {
        // 关闭 sink
        try {
            sinks.close();
        } catch (Exception ex) {
            LOGGER.error("Fail close media sinks", ex);
        } finally {
            IOUtils.closeQuietly(stratrgy);
        }

        // 删除本地缓冲区的文件
        buffer.clear();
    }

    @Override
    protected final void finalize() throws Throwable {
        close();
    }

}
