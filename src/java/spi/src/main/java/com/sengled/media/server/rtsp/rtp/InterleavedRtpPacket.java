package com.sengled.media.server.rtsp.rtp;

/**
 * @author las
 * @date 18-9-28
 */
public interface InterleavedRtpPacket extends RtpPacket, RtpChannel {

    @Override
    InterleavedRtpPacket copy();

    @Override
    InterleavedRtpPacket duplicate();

    @Override
    InterleavedRtpPacket retain();

    @Override
    InterleavedRtpPacket retain(int increment);
}
