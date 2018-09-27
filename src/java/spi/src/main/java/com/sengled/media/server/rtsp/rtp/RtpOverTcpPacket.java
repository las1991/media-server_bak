package com.sengled.media.server.rtsp.rtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

import java.util.List;

/**
 * @author las
 * @date 18-9-21
 */
public class RtpOverTcpPacket extends RtpPacket {

    private FramingPacket framingPacket;

    public RtpOverTcpPacket(FramingPacket framingPacket, ByteBuf content, int version, boolean padding, boolean extension, int cc, boolean marker, int payloadType, int seqNumber, long time, long syncSource, List<Long> cSources, int profile, int headerExtensionLength, byte[] headerExtension) {
        super(content, version, padding, extension, cc, marker, payloadType, seqNumber, time, syncSource, cSources, profile, headerExtensionLength, headerExtension);
        this.framingPacket = channel;
    }

    private RtpOverTcpPacket(ByteBuf content, RtpPacket that) {
        super(content, that);
        this.channel = that.getChannel();
    }

    @Override
    public int getChannel() {
        return framingPacket.getChannel();
    }

    @Override
    public RtpOverTcpPacket copy() {
        return new RtpOverTcpPacket(content().copy(), this);
    }

    @Override
    public ByteBufHolder duplicate() {
        return new RtpOverTcpPacket(content().duplicate(), this);
    }
}
