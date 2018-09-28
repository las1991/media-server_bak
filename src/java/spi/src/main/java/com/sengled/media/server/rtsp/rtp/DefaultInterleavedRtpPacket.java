package com.sengled.media.server.rtsp.rtp;

/**
 * @author las
 * @date 18-9-21
 */
public class DefaultInterleavedRtpPacket extends RtpPacket implements InterleavedRtpPacket {

    private final int channel;

    public DefaultInterleavedRtpPacket(int channel, RtpHeader header, RtpPayload payload) {
        super(header, payload);
        this.channel = channel;
    }

    @Override
    public int getChannel() {
        return this.channel;
    }

    @Override
    public String toString() {
        return "DefaultInterleavedRtpPacket{" +
                "channel=" + channel +
                ",rtpPacket=" + super.toString() +
                '}';
    }
}
