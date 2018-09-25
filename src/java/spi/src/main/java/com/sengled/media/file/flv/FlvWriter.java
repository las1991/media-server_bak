package com.sengled.media.file.flv;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicBoolean;
import com.sengled.media.FramePacket;
import com.sengled.media.StreamContext;
import com.sengled.media.file.MediaWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

public class FlvWriter implements MediaWriter {

	private StreamContext<?>[] contexts;
	private AtomicBoolean closed = new AtomicBoolean();
	private FlvOutputEncoder encoder = new FlvOutputEncoder(new StreamContext<?>[0]);
	
	private final OutputStream out;

	
	public FlvWriter(OutputStream out)  {
	    this.out = out;
	}

	@Override
	public MediaWriter open(StreamContext<?>[] contexts) {
	    encoder = new FlvOutputEncoder(contexts);
		return this;
	}
	
	@Override
	public void writeHeader() throws IOException {
	    ensureOpen();
    
	    
	    ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(); 
	    try {
	        encoder.writeFlvHeader(buf);
	        
	        buf.readBytes(this.out, buf.readableBytes());
	    } finally {
	        buf.release();
	    }
	}

 
	
	@Override
	public void writeFrame(int streamIndex, FramePacket frame) throws IOException {
	    ensureOpen();
	    
        
	    ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(); 
        try {
            encoder.encode(frame, buf);
            

            buf.readBytes(this.out, buf.readableBytes());
        } finally {
            buf.release();
        }
	}
	
	private FlvWriter ensureOpen() throws ClosedChannelException {
	    if (closed.get()) {
            throw new ClosedChannelException();
	    }
	    
	    return this;
	}
    
	@Override
	public void writeTail() throws IOException {
		
	}

	@Override
	public void flush() throws IOException {
		if (!closed.get()) {
			out.flush();
		}
	}
	
	@Override
	public void close() throws IOException {
		if (closed.compareAndSet(false, true)) {
		    out.close();
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		close();
	}
}
