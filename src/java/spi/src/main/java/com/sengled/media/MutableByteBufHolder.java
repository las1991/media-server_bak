package com.sengled.media;



import java.nio.ByteBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.DefaultByteBufHolder;

/**
 * ByteBuffer 是可以更改的
 * 
 * @author chenxh
 *
 */
public class MutableByteBufHolder extends DefaultByteBufHolder {

    public MutableByteBufHolder(ByteBuf data) {
        super(data);
    }

    public MutableByteBufHolder writeByte(int value) {
        content().writeByte(value);
        return this;
    }

    public MutableByteBufHolder writeShort(int value) {
        content().writeShort(value);
        return this;
    }

    public MutableByteBufHolder writeBytes(ByteBuf buf) {
        content().writeBytes(buf);

        return this;
    }


    public MutableByteBufHolder writeBytes(byte[] buf) {
        content().writeBytes(buf);

        return this;
    }

    public MutableByteBufHolder writeBytes(ByteBuffer buf) {
        content().writeBytes(buf);

        return this;
    }

    @Override
    public MutableByteBufHolder retain() {
        super.retain();

        return this;
    }

    @Override
    public MutableByteBufHolder retain(int increment) {
        super.retain(increment);
        return this;
    }


    public ByteBufAllocator alloc() {
        return content().alloc();
    }
}
