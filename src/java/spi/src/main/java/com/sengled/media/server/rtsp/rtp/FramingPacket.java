package com.sengled.media.server.rtsp.rtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

/**
 * @author las
 * @date 18-9-27
 */
public class FramingPacket extends DefaultByteBufHolder {

    private int channel;

    public FramingPacket(int channel, ByteBuf data) {
        super(data);
        this.channel = channel;
    }

    public int getChannel() {
        return channel;
    }

    public int getLength() {
        return content().readableBytes();
    }
}
