package com.sengled.media.file;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import com.sengled.media.FramePacket;
import com.sengled.media.StreamContext;

public interface MediaWriter extends Closeable, Flushable {

	public MediaWriter open(StreamContext<?>[] contexts);
	
	public void writeHeader() throws IOException;
	
	public void writeFrame(int streamIndex, FramePacket frame) throws IOException;
	
	public void writeTail() throws IOException;
	
	@Override
	void close() throws IOException;
	
	@Override
	void flush() throws IOException;
	
}