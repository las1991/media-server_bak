package com.sengled.media.server.rtsp.rtp.handler;

import com.sengled.media.server.rtsp.DefaultRtspMediaSink;
import com.sengled.media.server.rtsp.RtspMediaSink;
import com.sengled.media.server.rtsp.RtspServerContext;
import com.sengled.media.server.rtsp.RtspSession;
import com.sengled.media.server.rtsp.rtp.RtpPacket;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * @author las
 * @date 18-9-19
 */
public class RtpHandler extends SimpleChannelInboundHandler<RtpPacket> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpHandler.class);

    private final RtspServerContext serverContext;
    private final RtspSession session;
    private final RtpDePacketizer<?>[] rtpDePacketizers;

    private RtspMediaSink rtspMediaSink;

    public RtpHandler(RtspServerContext serverContext, RtspSession session, RtpDePacketizer<?>[] rtpDePacketizers) {
        this.serverContext = serverContext;
        this.session = session;
        this.rtpDePacketizers = rtpDePacketizers;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        this.rtspMediaSink = new DefaultRtspMediaSink(serverContext, session, rtpDePacketizers, ctx);
        rtspMediaSink.start();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        rtspMediaSink.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RtpPacket rtpPacket) throws Exception {
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

        rtspMediaSink.onRtp(rtpPacket.retain());
    }

}
