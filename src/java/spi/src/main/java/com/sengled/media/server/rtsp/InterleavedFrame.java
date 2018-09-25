package com.sengled.media.server.rtsp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

/**
 * Rtsp Interleaved Frame
 * <p>
 * RTP Over TCP
 * 
 * @author 陈修恒
 * @date 2016年4月29日
 */
public final class InterleavedFrame extends DefaultByteBufHolder {
    private int channel;
    
    public InterleavedFrame(int channel, ByteBuf payload) {
        super(payload);
        this.channel = channel;
    }
    
    public InterleavedFrame(InterleavedFrame frame) {
        this(frame.channel(), frame.content());
    }
    
    
    public int channel() {
        return channel;
    }

	public byte[] toBytes() {
        ByteBuf duplicate = content().duplicate();

        byte[] dst = new byte[duplicate.readableBytes()];
        duplicate.readBytes(dst);
        
        return dst;
	}
    
    @Override
    public InterleavedFrame copy() {
        return new InterleavedFrame(channel, content().copy());
    }
    
    /* (non-Javadoc)
     * @see com.sengled.cloud.mediaserver.codec.rtsp.InterleavedFrame#duplicate()
     */
    @Override
    public InterleavedFrame duplicate() {
        return new InterleavedFrame(channel, content().duplicate());
    }
    
    /* (non-Javadoc)
     * @see com.sengled.cloud.mediaserver.codec.rtsp.InterleavedFrame#retain()
     */
    @Override
    public InterleavedFrame retain() {
        content().retain();
        return this;
    }
    
    /* (non-Javadoc)
     * @see com.sengled.cloud.mediaserver.codec.rtsp.InterleavedFrame#retain(int)
     */
    @Override
    public InterleavedFrame retain(int increment) {
        content().retain(increment);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{").append(getClass().getSimpleName())
            .append(", refCnt=").append(refCnt())
            .append(", channel=").append(channel)
            .append(", bytes=").append(content().readableBytes());
        buf.append("}");
        return buf.toString();
    }

    
}

