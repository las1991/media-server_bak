package com.sengled.media.server.rtsp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtspServerPrepareHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtspServerPrepareHandler.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {
        if (evt instanceof IdleStateEvent) {
            final IdleStateEvent idleEvent = (IdleStateEvent) evt;

            ctx.close().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future)
                        throws Exception {
                    LOGGER.warn("close channel for {}{}", idleEvent.isFirst() ? "first " : "", idleEvent.state());
                }
            });
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.close().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                LOGGER.error("auto closed for {}", cause.getMessage(), cause);
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        // 记录 ssl 握手的协议
        SslHandler sslHandler = ctx.channel().pipeline().get(SslHandler.class);
        if (null != sslHandler) {
            String cipherSuite = sslHandler.engine().getSession().getCipherSuite();
            String client = null;
            String token = null;
            if (msg instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) msg;
                client = request.headers().get("Client");
                if (null == client) {
                    client = request.headers().get("User-Agent");
                }
                token = FilenameUtils.getName(request.getUri());
            }
            LOGGER.debug("[{}] with {} ({}), <{}, {}>", token, cipherSuite, client, ctx.channel().localAddress(), ctx.channel().remoteAddress());
        }

        ctx.pipeline().remove(this);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
}
