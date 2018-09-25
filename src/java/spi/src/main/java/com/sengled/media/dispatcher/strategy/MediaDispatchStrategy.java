package com.sengled.media.dispatcher.strategy;

import java.io.Closeable;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.FramePacket;
import com.sengled.media.StreamContext;

/**
 * 音视频分发策略
 * 
 * @author chenxh
 */
public abstract class MediaDispatchStrategy implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaDispatchStrategy.class);
    

    protected final String token;

    
    
    MediaDispatchStrategy(String token) {
        super();
        this.token = token;
    }

    public abstract void read(FramePacket frame, List<Object> out);
    public abstract void close();
    
    public static MediaDispatchStrategy newInstance(String token, StreamContext<?>[] streams) {
        LOGGER.info("[{}] {}", token, streams);
        
        if (streams.length > 0) {
            return new MultistreamMediaDispatchStrategy(token, streams);
        } else {
            // 老对讲，什么都没有
            return new UnknownMediaDispatchStrategy(token);
        }
    }
    
    

}
