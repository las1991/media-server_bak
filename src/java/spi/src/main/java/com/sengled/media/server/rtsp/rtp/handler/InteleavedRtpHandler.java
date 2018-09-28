package com.sengled.media.server.rtsp.rtp.handler;

import com.sengled.media.server.rtsp.RtspMediaSink;
import com.sengled.media.server.rtsp.rtp.InterleavedRtpPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * @author las
 * @date 18-9-19
 */
public class InteleavedRtpHandler extends SimpleChannelInboundHandler<InterleavedRtpPacket> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteleavedRtpHandler.class);

    private final RtspMediaSink rtspMediaSink;

    public InteleavedRtpHandler(RtspMediaSink rtspMediaSink) {
        super(false);
        this.rtspMediaSink = rtspMediaSink;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        rtspMediaSink.start();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        rtspMediaSink.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, InterleavedRtpPacket rtpPacket) throws Exception {
        //TODO 比较性能
        ctx.channel().eventLoop().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                return null;
            }
        });

        ctx.channel().eventLoop().execute(new Runnable() {
            @Override
            public void run() {

            }
        });
        rtspMediaSink.onRtp(rtpPacket);
    }

}
