package com.sengled.media.server.rtsp.rtp.packetizer.g711;

import com.sengled.media.MediaCodec;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.MediaCodecExtra;
import com.sengled.media.server.rtsp.rtp.RtpPacket;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;

import java.util.List;

/**
 * G711
 *
 * @author chenxh
 */
public class G711RtpDePacketizer extends RtpDePacketizer<MediaCodecExtra> {

    public G711RtpDePacketizer(int streamIndex, Rational timeUnit) {
        super(streamIndex, MediaCodec.G711, null, timeUnit);
    }

    @Override
    protected int dePacket(StreamContext<MediaCodecExtra> ctx, RtpPacket rtpPacket, List<Object> out) {
        out.add(newFramePacket().writeBytes(rtpPacket.content()));
        return AUDIO;
    }
}
