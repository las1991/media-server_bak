package com.sengled.media.server.rtsp.rtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

/**
 * @author las
 * @date 18-9-27
 */
public class FramingRtpPacket extends DefaultByteBufHolder implements RtpChannel {

    private final int channel;
    private final int length;

    public FramingRtpPacket(int channel, int length, ByteBuf data) {
        super(data);
        this.channel = channel;
        this.length = length;
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
}
