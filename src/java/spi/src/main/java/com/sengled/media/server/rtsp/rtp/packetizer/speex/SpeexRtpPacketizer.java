package com.sengled.media.server.rtsp.rtp.packetizer.speex;

import com.sengled.media.FramePacket;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.MediaCodecExtra;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpPacketizer;

import java.util.List;

public class SpeexRtpPacketizer extends RtpPacketizer<MediaCodecExtra> {

    public SpeexRtpPacketizer(int streamIndex, StreamContext<MediaCodecExtra> src) {
        super(streamIndex, Rational.valueOf(src.getSampleRate()), src);
    }
    
    
    protected void packet0(FramePacket frame, List<Object> out) {
        out.add(newRTP().writeBytes(frame.content()));
    }
}
