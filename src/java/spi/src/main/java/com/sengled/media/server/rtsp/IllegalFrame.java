package com.sengled.media.server.rtsp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.DefaultByteBufHolder;

/**
 * 非法的数据帧
 * 
 * @author chenxh
 */
public class IllegalFrame  extends DefaultByteBufHolder {

    public IllegalFrame(ByteBuf data) {
        super(data);
    }
    
    @Override
    public IllegalFrame copy() {
        return new IllegalFrame(content().copy());
    }
    
    @Override
    public IllegalFrame duplicate() {
        return new IllegalFrame(content().duplicate());
    }
    
    @Override
    public IllegalFrame retain() {
        content().retain();
        return this;
    }
    
    @Override
    public ByteBufHolder retain(int increment) {
        content().retain(increment);
        return this;
    }
}
