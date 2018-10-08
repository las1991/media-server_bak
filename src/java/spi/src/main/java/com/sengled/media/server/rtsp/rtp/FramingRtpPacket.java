package com.sengled.media.server.rtsp.rtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.DefaultByteBufHolder;

/**
 * @author las
 * @date 18-9-27
 */
public class FramingRtpPacket implements ByteBufHolder, RtpChannel {

    private final int channel;
    private final int length;

    private ByteBuf content;

    public FramingRtpPacket(int channel, int length, ByteBuf content) {
        this.channel = channel;
        this.length = length;
        this.content = content;
    }

    public int getChannel() {
        return channel;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "FramingRtpPacket{" +
                "channel=" + channel +
                ", length=" + length +
                '}';
    }

    @Override
    public ByteBuf content() {
        return content;
    }

    @Override
    public FramingRtpPacket copy() {
        return new FramingRtpPacket(this.channel, this.length, content.copy());
    }

    @Override
    public FramingRtpPacket duplicate() {
        return new FramingRtpPacket(this.channel, this.length, content.duplicate());
    }

    @Override
    public int refCnt() {
        return content.refCnt();
    }

    @Override
    public FramingRtpPacket retain() {
        return new FramingRtpPacket(this.channel, this.length, content.retain());
    }

    @Override
    public FramingRtpPacket retain(int increment) {
        return new FramingRtpPacket(this.channel, this.length, content.retain(increment));
    }

    @Override
    public boolean release() {
        return content.release();
    }

    @Override
    public boolean release(int decrement) {
        return content.release(decrement);
    }
}
