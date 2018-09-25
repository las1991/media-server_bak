package com.sengled.media.server.rtsp.rtp.packetizer.h264;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.model.Size;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class MediaDescriptionParser implements com.sengled.media.server.rtsp.rtp.MediaDescriptionParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaDescriptionParser.class);
	
	@Override
	public boolean accept(Media media, RtpMapAttribute rtpmap) {
		return "H264".equals(rtpmap.getCodec());
	}
	
	@Override
	public H264DePacketizer parse(RtpMapAttribute rtpmap, MediaDescription md, int streamIndex) throws SdpParseException {

		int width = 0, height = 0;
		double frameRate = 0;
		AVCDecoderConfigurationRecord record = new AVCDecoderConfigurationRecord();
		int packetizationMode = 0;
		
		String fmtpValue = md.getAttribute("fmtp");
		if(null != fmtpValue) {
			Matcher matcher;
			matcher = Pattern.compile("([^;\\s=]+)=(([^;\\s,]+)(,([^;\\s]+))?)").matcher(fmtpValue);

			ByteBuffer profile = null, sps = null, pps = null;
			
			// packetization-mode=1;profile-level-id=4D001F;sprop-parameter-sets=Z00AH9oBQBbpUgAAAwACAAADAGTAgAC7fgAD9H973wvCIRqA,aM48gA==
			while(matcher.find()) {
				String key = matcher.group(1).toLowerCase();
				String value = matcher.group(2);
				if ("profile-level-id".equals(key)) {
					profile = RtpDePacketizer.readHex(value);
				} else if ("sprop-parameter-sets".equals(key)) {
					sps = RtpDePacketizer.readBase64(matcher.group(3));
					pps = RtpDePacketizer.readBase64(matcher.group(5));
					
					
					ByteBuffer duplicate = sps.duplicate();
					duplicate.get(); // skip nal type
					SeqParameterSet seqParameterSet = SeqParameterSet.read(duplicate);
					Size size = H264Utils.getPicSize(seqParameterSet);
					
					width = size.getWidth();
					height =size.getHeight();
					
					// 固定帧率
					if (null != seqParameterSet.vuiParams 
							&& seqParameterSet.vuiParams.timing_info_present_flag
							&& seqParameterSet.vuiParams.fixed_frame_rate_flag) {
						frameRate = 1.0 * seqParameterSet.vuiParams.time_scale / (2 * seqParameterSet.vuiParams.num_units_in_tick);
					}
					if (LOGGER.isDebugEnabled()) {
					    LOGGER.debug("size:{}*{}", width, height);
						LOGGER.debug("framerate:{}", frameRate);
						LOGGER.debug("SPS:\n{}", ByteBufUtil.prettyHexDump(Unpooled.wrappedBuffer(sps)));
						LOGGER.debug("PPS:\n{}", ByteBufUtil.prettyHexDump(Unpooled.wrappedBuffer(pps)));
					}
				} else if ("packetization-mode".equals(key)) {
					packetizationMode = Integer.valueOf(value);
				} else {
					LOGGER.info("ignored [{}={}]", key, value);
				}
			}

			record.setSPS_PPS(sps, pps, profile);
		}
		

		H264DePacketizer ctx = new H264DePacketizer(streamIndex, record, Rational.valueOf(rtpmap.getClockRate()));
		ctx.setWidth(width);
		ctx.setHeight(height);
		ctx.setFrameRate(frameRate);
		ctx.setPacketizationMode(packetizationMode);
		return ctx;
	}

}
