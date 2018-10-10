package com.sengled.media.server.rtsp.rtp;

/**
 * @author las
 * @date 18-10-10
 */
public interface RtpPacket extends RtpHeader, RtpBody {

    @Override
    RtpPacket copy();

    @Override
    RtpPacket duplicate();

    @Override
    RtpPacket retain();

    @Override
    RtpPacket retain(int increment);
}
