package com.sengled.media.jcodec.h264;

import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

/**
 * 用来解析 H264 的 nal 数据包
 * @author chenxh
 */
public class H264Parser {
	private static final Logger LOGGER = LoggerFactory.getLogger(H264Parser.class);

	public static final int NAL_UNKNOWN    = -1;
	public static final int NAL_CODED_SLICE_OF_NON_IDR_PICTURE    = 1;
	public static final int NAL_KEY = 5;
	public static final int NAL_SEI = 6;
	public static final int NAL_SPS = 7;
	public static final int NAL_PPS = 8;
	public static final int NAL_ACCESS_UNIT_DELIMITER = 9;
	public static final int NAL_STAP_A = 24; // STAP-A (one packet, multiple nals)
	public static final int NAL_FU_A = 28; // FU-A (fragmented nal)
	
	private ByteBuffer bytes;
	private SeqParameterSet sps;

	public int feed(ByteBuf nal) {
		try {
			if (null != nal && nal.readableBytes() > 2) {
				int nalType = nal.readByte() & 0x1F;
				ByteBuffer nalData;
				switch(nalType) {
				case NAL_SPS:
					nalData = readNal(nal);
					SeqParameterSet sps = SeqParameterSet.read(nalData);
					if (null != sps 
							&& sps.pic_width_in_mbs_minus1 > 1 
							&& sps.pic_height_in_map_units_minus1 > 1) {
						this.sps = sps;
						
						LOGGER.debug("size = {} * {}", getWidth(), getHeight());
					} else {
						LOGGER.error("illegal sps");
					}
					break;
				}
				return nalType;
			}
		} catch(Exception ex) {
			
		} finally {
			ReferenceCountUtil.release(nal);
		}
		
		return NAL_UNKNOWN;
	}
	
	
	private ByteBuffer readNal(ByteBuf nal) {
		if (null == bytes || bytes.capacity() < nal.readableBytes()) {
			bytes = ByteBuffer.allocate(nal.readableBytes());
		} else {
			bytes.clear();
		}

		bytes.limit(nal.readableBytes());
		nal.readBytes(bytes);
		bytes.flip();
		
		return bytes;
	}


	public int getWidth() {
		if (null != sps) {
			return ((sps.pic_width_in_mbs_minus1 +1)*16) - sps.frame_crop_left_offset*2 - sps.frame_crop_right_offset*2;
		}
		return -1;
	}


	public int getHeight() {
		if (null != sps) {
			 return ((sps.frame_mbs_only_flag ? 1 : 2)* (sps.pic_height_in_map_units_minus1 +1) * 16) - (sps.frame_crop_top_offset * 2) - (sps.frame_crop_bottom_offset * 2);
		}
		return -1;
	}
}
