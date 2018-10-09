package com.sengled.media.server.rtsp.rtcp;

/**
 * @author las
 * @date 18-10-9
 */
public class DefaultInterleavedRtcpPacket extends DefaultRtcpPacket implements InterleavedRtcpPacket {

    private final int channel;

    public DefaultInterleavedRtcpPacket(int channel) {
        this.channel = channel;
    }

    @Override
    public int getChannel() {
        return channel;
    }
}
