package com.sengled.media.server.rtsp.servlet;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.server.NettySupport;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * 处理 连接 的接口
 * 
 * @author chenxh
 */
public class ChannelHandlerContextHolder extends NettySupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelHandlerContextHolder.class);
    
    private ChannelHandlerContext context;
    private final AtomicBoolean closed = new AtomicBoolean();
   
    protected ChannelHandlerContextHolder(ChannelHandlerContext ctx) {
        this.context = ctx;
    }

    final public SocketAddress remoteAddress() {
        return channel().remoteAddress();
    }

    final public SocketAddress localAddress() {
        return channel().localAddress();
    }
    
    final public Channel channel() {
    	return context.channel();
    }
    
    final public ChannelHandlerContext channelHandlerContext() {
        return context;
    }
    
    final public ChannelFuture write(Object src) {
        return context.write(src);
    }
    
    final public void flush() {
        context.flush();
    }
    
    final public ChannelFuture writeAndFlush(Object src) {
        return context.writeAndFlush(src);
    }

    final public EventLoop eventLoop() {
        return context.channel().eventLoop();
    }
  
    public final boolean isClosed() {
        return closed.get();
    }
    
    public final boolean isWritable() {
        return channel().isWritable();
    }

    protected final void ensureOpen() throws IOException {
        if(closed.get()) {
            throw new IOException(this + " closed");
        }
    }
    
    protected void beforeClose() throws IOException {}
    
    
    
    final public ChannelFuture close() {
        // 做一些清理工作，比如发送 BYE
        try {
            beforeClose();
        } catch (Exception e) {
            LOGGER.error("Fail close", e.getMessage(), e);
        }

        // 准备释放内存
        final ChannelPromise promise = context.newPromise();
        try{
            // 把缓存中的东西刷出去
            context.flush();

            onClosed(promise);
        } catch (Exception e) {
            LOGGER.error("Fail setup promis before close for {}", e.getMessage(), e);
        } finally {
            promise.addListener(new GenericFutureListener<Future<? super Void>>() {
                public void operationComplete(Future<? super Void> future) throws Exception {
                    closed.set(true);
                };
            });
        }
        
        return context.close(promise);
    }
    

    protected void onClosed(ChannelPromise promise) throws IOException {
        
    }
}
