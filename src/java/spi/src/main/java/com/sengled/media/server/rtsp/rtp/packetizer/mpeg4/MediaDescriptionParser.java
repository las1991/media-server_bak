package com.sengled.media.server.rtsp.rtp.packetizer.mpeg4;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;
import org.jcodec.common.io.BitReader;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.MediaCodec;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;

public class MediaDescriptionParser implements com.sengled.media.server.rtsp.rtp.MediaDescriptionParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaDescriptionParser.class);

	@Override
	public boolean accept(Media media, RtpMapAttribute rtpmap) {
		return "MPEG4-GENERIC".equalsIgnoreCase(rtpmap.getCodec());
	}
	
	@Override
	public Mpeg4RtpDePacketizer parse(RtpMapAttribute rtpmap, MediaDescription md, int streamIndex) throws SdpParseException {
		MediaCodec codec = null;
		
		Mpeg4PacketConfig config = new Mpeg4PacketConfig();
		AudioSpecificConfig mediaExtra = new AudioSpecificConfig();
		
		
		// a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr;
		// config=139056e5a54800; SizeLength=13; IndexLength=3;
		// IndexDeltaLength=3; Profile=1;
		String fmtp = md.getAttribute("fmtp");
		Matcher matcher = Pattern.compile("([^;\\s=]+)=([^;\\s]+)").matcher(fmtp);
		while (matcher.find()) {
			String key = matcher.group(1);
			String value = matcher.group(2);

			if ("streamtype".equalsIgnoreCase(key)) {
				config.streamtype = Integer.valueOf(value);
			} else if ("profile-level-id".equalsIgnoreCase(key)) {
				config.profileLevelId = Integer.valueOf(value);
			} else if ("mode".equalsIgnoreCase(key)) {
				config.mode = value;
				if ("AAC-hbr".equals(value)) {
					codec = MediaCodec.AAC;
				}
			} else if ("config".equalsIgnoreCase(key)) {
                mediaExtra.config = RtpDePacketizer.readHex(value);;
			} else if ("SizeLength".equalsIgnoreCase(key)) {
				config.sizeLength = Integer.valueOf(value);
			} else if ("IndexLength".equalsIgnoreCase(key)) {
				config.indexLength = Integer.valueOf(value);
			} else if ("IndexDeltaLength".equalsIgnoreCase(key)) {
				config.indexDeltaLength = Integer.valueOf(value);
			} else if ("Profile".equalsIgnoreCase(key)) {
				config.profile = Integer.valueOf(value);
			} else {
				LOGGER.info("ignored [{}={}]", key, value);
			}
		}

		if (null != codec) {
			int sampleRate = 44100;
			int channels = 2;
			
			BitReader reader = new BitReader(ByteBuffer.wrap(mediaExtra.getConfig()));
			reader.readNBit(5); // object type
			sampleRate = AAC.AUDIO_SAMPLING_RATES[reader.readNBit(4)];
			channels = reader.readNBit(4);
			
			Mpeg4RtpDePacketizer ctx = new Mpeg4RtpDePacketizer(streamIndex, mediaExtra, Rational.valueOf(rtpmap.getClockRate()), config);
			ctx.setNumChannels(channels);
			ctx.setSampleRate(sampleRate);
			ctx.setSampleSize(1024);
			return ctx;
		} else {
			return null;
		}
	}

}
