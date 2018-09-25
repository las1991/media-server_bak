package com.sengled.media.server.rtsp.rtp;

import org.mobicents.media.server.impl.rtp.statistics.InterleavedRtpPacket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.util.ReferenceCounted;

/**
 * 一个 RTP 包
 * 
 * @author chenxh
 */
public interface RtpPacketI extends ReferenceCounted, InterleavedRtpPacket {
	
	long getTime();
	
	int getSeqNumber();

	boolean getMarker();
	
	/**
	 * @return true 表示 rtp 有扩展头
	 */
	public int getFlags();
	
	@Override
	int getPayloadLength();


    int getVersion();
    
	@Override
	long getSyncSource();
	

    RtpPacketI copyFast();
    
	
    /**
     * Return the data which is held by rtp.
     */
    ByteBuf content();
	
    /**
     * Duplicate the {@link ByteBufHolder}. Be aware that this will not automatically call {@link #retain()}.
     */
    RtpPacketI duplicate();

    @Override
    RtpPacketI retain();

    @Override
    RtpPacketI retain(int increment);

    int getPayloadType();

    void setTime(long rtpTime);
}
