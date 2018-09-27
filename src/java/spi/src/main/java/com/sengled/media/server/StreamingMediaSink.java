package com.sengled.media.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.MediaDispatcher;
import com.sengled.media.MediaSink;
import com.sengled.media.MediaSource;
import com.sengled.media.StreamContext;
import com.sengled.media.server.rtsp.servlet.ChannelHandlerContextHolder;
import io.netty.channel.ChannelHandlerContext;

/**
 * 具有丢包逻辑的视频 MediaSink
 * 
 * @author chenxh
 */
public abstract class StreamingMediaSink extends ChannelHandlerContextHolder implements MediaSink  {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingMediaSink.class);
    
    private final MediaSource source;
    private final StreamContext<?>[] streams;
    
    
    private boolean hasVideo;
    private State state = State.WAIT_KEY;
    
    protected abstract void writeInterleaved(StreamContext<?> ctx, MutableFramePacket frame);
	
    protected StreamingMediaSink(ChannelHandlerContext ctx, MediaSource source, StreamContext<?>[] streams) {
        super(ctx);
    	this.source = source;
        this.streams = streams;
        for (int i = 0; i < streams.length; i++) {
            if (null != streams[i] && streams[i].getCodec().isVideo()) {
                hasVideo = true;
            }
        }
    }

    @Override
    public final void start() {
    	doStart(source);
    }
    
    protected void doStart(MediaSource source) {
        StreamingMediaSink sink = this;
        source.submit(new Function<MediaDispatcher, Boolean>() {
            @Override
            public Boolean apply(MediaDispatcher dispatcher) {
                //初始化成功后加入
                return dispatcher.addMediaSink(sink);
            }
        });
    }
    
    @Override
    protected final void beforeClose() {
        StreamingMediaSink sink = this;
        source.submit(new Function<MediaDispatcher, Boolean>() {
            @Override
            public Boolean apply(MediaDispatcher t) {
                return t.removeMediaSink(sink);
            }
        });
        
        beforeClose0();
    }
    
    protected void beforeClose0() {}
    
	@Override
	protected void finalize() throws Throwable {
		close();
	}

	@Override
	public void setup(List<MutableFramePacket> frames) throws IOException {
	    try {
            if (has_B_Frame(frames)) {
                for (MutableFramePacket mutableFramePacket : frames) {
                    onFrame(mutableFramePacket.retain());
                }
               
                return;  // 有 B 帧， 直接输出
            }
            
	        // 找最大时间
	        StreamContext<? extends MediaCodecExtra>[] streams = source.getStreamContexts();
            List<Long> maxTimes =
            Arrays.asList(streams).stream().map(stream -> {
                long maxTime = -1;
                final int streamIndex = stream.getStreamIndex();
                for (MutableFramePacket mutableFramePacket : frames) {
                    if (streamIndex == mutableFramePacket.getStreamIndex()) {
                        maxTime = mutableFramePacket.getTime();
                    }
                }
                return maxTime;
            }).collect(Collectors.toList());
            
            // 输出关键帧, 以及一个音频帧
            for (StreamContext<? extends MediaCodecExtra> t : streams) {
                final int streamIndex = t.getStreamIndex();
                long maxTime = maxTimes.get(streamIndex);
                switch (t.getMediaType()) {
                    case VIDEO:
                        for (MutableFramePacket mutableFramePacket : frames) {
                            if (streamIndex == mutableFramePacket.getStreamIndex()) {
                                mutableFramePacket.setTime(maxTime);
                                onFrame(mutableFramePacket.retain());
                            }
                        }
                        break;
                    case AUDIO:
                        LinkedList<MutableFramePacket> threeFrame = new LinkedList<>();
                        for (MutableFramePacket mutableFramePacket : frames) {
                            if (streamIndex == mutableFramePacket.getStreamIndex()) {
                                threeFrame.add(mutableFramePacket);
                            }
                        }

                        // 
                        if(!threeFrame.isEmpty()) {
                            onFrame(threeFrame.getLast().retain());
                        }
                    default:
                        break;
                }
            
            }
	    } finally {
            release(frames);
        }
	}
	
   private boolean has_B_Frame(List<MutableFramePacket> frames) {
        long lastFrameTime = -1;
        for (StreamContext<? extends MediaCodecExtra> stream :  source.getStreamContexts()) {
            if (null == stream || !stream.getCodec().isVideo()) {
               continue;
            }
            
            for (MutableFramePacket frame : frames) {
                if (frame.getStreamIndex() == stream.getStreamIndex() ) {
                    if (frame.getTime() < lastFrameTime) {
                        return true;
                    }
                    
                    lastFrameTime = frame.getTime();
                }
            }
        }
        
        return false;
    }
   
    @Override
    public final void onFrame(MutableFramePacket frame) throws IOException {
        try {
        	ensureOpen();
        	
            switch (state) {
            case WAIT_KEY:
                if (!hasVideo || frame.isKeyFrame()) {
                    withState(State.WRITABLE).writeInteleaved(frame);
                }

                break;
            case WRITABLE:
                if (isWritable()) {
                    writeInteleaved(frame);
                } else {
                    withState(State.BUFFER_FULL); // buffer full;
                }
                break;
            case BUFFER_FULL:
                if (isWritable()) {
                    if (frame.isKeyFrame()) {
                        withState(State.WRITABLE).writeInteleaved(frame); // writable, and is KEY
                    } else {
                        withState(State.WAIT_KEY); // writable, wait KEY
                    }
                }
                
                break;
            default:
                throw new IOException("illegal state[" + state + "]");
            }
        } finally {
            frame.release();
        }
    }
    
    private StreamingMediaSink withState(State state) {
        this.state = state;

        switch (state) {
            case WRITABLE:
                LOGGER.debug("[{}] {}, sink = {}", source.getToken(), state, channel().remoteAddress());
                break;
            default:
                LOGGER.info("[{}] {}, sink = {}", source.getToken(), state, channel().remoteAddress());
                break;
        }
        
        return this;
    }


    private StreamingMediaSink writeInteleaved(MutableFramePacket frame) throws IOException {
    	writeInterleaved(streams[frame.getStreamIndex()], frame);
        
        return this;
    }


	public String getToken() {
		return source.getToken();
	}
	
	
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        buf.append(getClass().getSimpleName());
        buf.append(", uri=").append(getToken());
        buf.append(", remote=").append(channel().remoteAddress());
        buf.append("}");

        return buf.toString();
    }

    
    private enum State {
        WAIT_KEY,
        WRITABLE,
        BUFFER_FULL;
    }
}