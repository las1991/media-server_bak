package com.sengled.media.server.rtsp.rtp.packetizer.g711;

import java.util.List;
import com.sengled.media.MediaCodec;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.MediaCodecExtra;
import com.sengled.media.server.rtsp.rtp.RtpPacketI;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;

/**
 * G711
 * 
 * @author chenxh
 *
 */
public class G711RtpDePacketizer extends RtpDePacketizer<MediaCodecExtra> {

	public G711RtpDePacketizer(int streamIndex, Rational timeUnit) {
        super(streamIndex, MediaCodec.G711, null, timeUnit);
    }

    @Override
	protected int dePacket(StreamContext<MediaCodecExtra> ctx, RtpPacketI rtpPkt, List<Object> out) {
		out.add(newFramePacket().writeBytes(rtpPkt.content()));
		return AUDIO;
	}
}
