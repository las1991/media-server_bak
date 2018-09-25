package com.sengled.media;

import org.mobicents.media.server.spi.MediaType;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.MediaCodecExtra;

/**
 * 视频流的上下文
 * 
 * @author chenxh
 */
public interface StreamContext <T extends MediaCodecExtra> {
	
	public MediaType getMediaType();
	
	/**
	 * 视频流的配置信息
	 * 
	 * @return null 表示没有配置
	 */
	public T getExtra();
	
	
	/**
	 * 解码器名
	 * 
	 * @return
	 */
	public MediaCodec getCodec();
	
	/**
	 * 流名称
	 * 
	 * @return
	 */
	public int getStreamIndex();

	public int getWidth();

	public int getHeight();


	public double getFrameRate();


	public int getSampleRate();


	public int getNumChannels();

	public int getSampleSize();

    public Rational getTimeUnit();
}
