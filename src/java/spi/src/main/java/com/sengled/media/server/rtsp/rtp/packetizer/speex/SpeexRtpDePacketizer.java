package com.sengled.media.server.rtsp.rtp.packetizer.speex;

import com.sengled.media.MediaCodec;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.MediaCodecExtra;
import com.sengled.media.server.rtsp.rtp.RtpPacket;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;

import java.util.List;

public class SpeexRtpDePacketizer extends RtpDePacketizer<MediaCodecExtra> {

    public SpeexRtpDePacketizer(int streamIndex, Rational timeUnit) {
        super(streamIndex, MediaCodec.SPEEX, null, timeUnit);
    }

    @Override
    protected int dePacket(StreamContext<MediaCodecExtra> ctx, RtpPacket rtpPkt, List<Object> out) {
        out.add(newFramePacket().writeBytes(rtpPkt.content()));
        return AUDIO;
    }
}
