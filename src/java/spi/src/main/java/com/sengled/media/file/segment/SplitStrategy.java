package com.sengled.media.file.segment;

import java.io.IOException;

import com.sengled.media.server.MutableFramePacket;

import io.netty.buffer.ByteBufAllocator;

public interface SplitStrategy {
	Segment nextSegment(ByteBufAllocator alloc, long nextFrameTime);

	boolean isNeedClose(Segment segment, MutableFramePacket next);

	/**
	 * @param segment
	 * @param next
	 * @return true 表示把 segment 关闭了
	 * @throws IOException 
	 */
	public void close(Segment segment) throws IOException;
}
