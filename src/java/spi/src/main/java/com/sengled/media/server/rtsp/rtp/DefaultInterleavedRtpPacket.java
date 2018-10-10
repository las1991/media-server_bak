package com.sengled.media.server.rtsp.rtp;

import io.netty.buffer.ByteBuf;

/**
 * @author las
 * @date 18-9-21
 */
public class DefaultInterleavedRtpPacket extends RtpPacketHolder implements InterleavedRtpPacket {

    private final int channel;

    public DefaultInterleavedRtpPacket(int channel, RtpHeader header, ByteBuf payload) {
        super(header, payload);
        this.channel = channel;
    }

    @Override
    public int getChannel() {
        return this.channel;
    }

    @Override
    public DefaultInterleavedRtpPacket copy() {
        return new DefaultInterleavedRtpPacket(this.channel, rtpHeader(), payload().copy());
    }

    @Override
    public DefaultInterleavedRtpPacket duplicate() {
        super.duplicate();
        return this;
    }

    @Override
    public DefaultInterleavedRtpPacket retain() {
        super.retain();
        return this;
    }

    @Override
    public DefaultInterleavedRtpPacket retain(int increment) {
        super.retain(increment);
        return this;
    }

    @Override
    public String toString() {
        return "DefaultInterleavedRtpPacket{" +
                "channel=" + channel +
                ",rtpPacket=" + super.toString() +
                '}';
    }
}
