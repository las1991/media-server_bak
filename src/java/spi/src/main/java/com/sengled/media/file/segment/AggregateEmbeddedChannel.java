package com.sengled.media.file.segment;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * 把多个 ByteBuf 切片合并成一个切片拷贝。
 * 
 * 这样能节省内存。
 * @author chenxh
 */
public class AggregateEmbeddedChannel extends EmbeddedChannel {
	private ByteBufAllocator alloc;
	
	public AggregateEmbeddedChannel(ByteBufAllocator alloc) {
		this.alloc = alloc;
	}

	@Override
	protected void doWrite(ChannelOutboundBuffer in) throws Exception {
		CompositeByteBuf composite = alloc.compositeBuffer(in.size());
		try {
	        for (;;) {
	            ByteBuf msg = (ByteBuf) in.current();
	            if (msg == null) {
	                break;
	            }
	            
	            composite.addComponent(true, msg.retain());
	            in.remove();
	        }

	        outboundMessages().add(composite.copy());
		} finally {
			composite.release();
		}
	}
}
