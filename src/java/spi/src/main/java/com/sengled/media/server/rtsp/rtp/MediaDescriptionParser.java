package com.sengled.media.server.rtsp.rtp;

import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import com.sengled.media.server.MediaCodecExtra;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;

/**
 * 从 MediaDescription 中提取视 Media 的关键配置信息
 * 
 * @author chenxh
 */
public interface MediaDescriptionParser {
	
	/**
	 * @param media
	 * @param rtpmap
	 * @return true 表示能够处理这里视频流
	 */
	boolean accept(Media media, RtpMapAttribute rtpmap);
	
	/**
	 * 构造出拆包器
	 * @param rtpmap
	 * @param md
	 * @param streamIndex
	 * 
	 * @return
	 * @throws SdpParseException
	 */
	RtpDePacketizer<? extends MediaCodecExtra> parse(RtpMapAttribute rtpmap, MediaDescription md, int streamIndex) throws SdpParseException;
}
