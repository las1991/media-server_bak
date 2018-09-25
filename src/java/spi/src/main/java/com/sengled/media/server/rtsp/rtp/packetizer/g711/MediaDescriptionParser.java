package com.sengled.media.server.rtsp.rtp.packetizer.g711;

import java.util.Vector;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import com.sengled.media.clock.Rational;

/**
 * 
 * MediaDescription:
 * <pre>
 * m=audio 0 RTP/AVP 8
 * b=AS:64
 * a=control:streamid=1
 * @author chenxh
 *
 */
public class MediaDescriptionParser implements com.sengled.media.server.rtsp.rtp.MediaDescriptionParser {
	@Override
	public boolean accept(Media media, RtpMapAttribute rtpmap) {
		try {
		    if("PCMA".equalsIgnoreCase(rtpmap.getCodec())) {
		        return true;
		    }
		    
			Vector<?> mediaFormats = media.getMediaFormats(true);
			for (Object format : mediaFormats) {
				return "8".equals(format.toString());
			}
		} catch(Exception ex){}

		return false;
	}

	@Override
	public G711RtpDePacketizer parse(RtpMapAttribute rtpmap, MediaDescription md, int streamIndex)
			throws SdpParseException {
		int numChannels = 1;
		int sampleRate = 8000;
		int sampleSize = 16;
		Rational timeUnit = Rational.valueOf(8000);
		
		G711RtpDePacketizer ctx = new G711RtpDePacketizer(streamIndex, timeUnit);
		ctx.setNumChannels(numChannels);
		ctx.setSampleRate(sampleRate);
		ctx.setSampleSize(sampleSize);
		return ctx;
	}

}
