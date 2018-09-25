package com.sengled.media.dispatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.MediaSink;
import com.sengled.media.MediaSinkGroup;
import com.sengled.media.server.MutableFramePacket;

public class DefaultMediaSinkGroup implements MediaSinkGroup {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMediaSinkGroup.class);

    private List<MediaSink> sinks = new CopyOnWriteArrayList<>();//使用并发安全的容器

    public DefaultMediaSinkGroup() {
    }


    public int dispatch(MutableFramePacket pkt) {
        try {
            // 执行 dispatch 操作
            return doDispatch(pkt);
        } finally {
            pkt.release();
        }
    }

    private int doDispatch(MutableFramePacket pkt) {
        int numSinks = 0;
        for (MediaSink sink : sinks) {
            if (null == sink) {
                continue;
            }
            

            try {
                // 考虑到 flv 时间戳只用 4 字节，为防止时间戳轮转，对长时间在线的需要断开
                if (!sink.isClosed()) {
                    sink.onFrame(pkt.retain().duplicate());
                    numSinks ++;
                } else {
                    sinks.remove(sink);
                }
            } catch (Exception ex) {
                LOGGER.error("Fail write to {} for {}", sink, ex.getMessage(), ex);
                boolean removed = sinks.remove(sink);
                if (removed) {
                    sink.close();
                }
            }
        }
        
        
        return numSinks;
    }

    @Override
    public void close() throws IOException {
        closeAndClear(sinks);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    private void closeAndClear(List<? extends MediaSink> sinks) {
        int numRemoved = 0;
        for (MediaSink sink : new ArrayList<>(sinks)) {
            try {
                boolean removed = sinks.remove(sink);
                if (removed) {
                    sink.close();
                    numRemoved ++;
                } 
            } catch (Exception e) {
                LOGGER.error("Fail close {}", sink, e);
            }
        }
        
        if (numRemoved > 0) {
            LOGGER.debug("remove {} sinks", numRemoved);
        }
    }

    @Override
    public boolean add(MediaSink sink) {
        return sinks.add(sink);
    }

    @Override
    public boolean remove(MediaSink sink) {
        return sinks.remove(sink);
    }

    @Override
    public List<MediaSink> getMediaSinks() {
        return sinks;
    }

    @Override
    public int size() {
        return sinks.size();
    }

    @Override
    public boolean isEmpty() {
        return size() <= 0;
    }
}
