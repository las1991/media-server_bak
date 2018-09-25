package com.sengled.media.server.rtsp.rtp.packetizer.g711;

import java.util.List;
import com.sengled.media.FramePacket;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.MediaCodecExtra;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpPacketizer;

public class G711RtpPacketizer extends RtpPacketizer<MediaCodecExtra> {

    public G711RtpPacketizer(int streamIndex, StreamContext<MediaCodecExtra> src) {
        super(streamIndex, Rational.$8_000, src);
    }
    
    
    protected void packet0(FramePacket frame, List<Object> out) {
        out.add(newRTP().writeBytes(frame.content()));
    }
}
