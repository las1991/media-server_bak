package com.sengled.media.server.rtsp.rtcp.handler;

import com.sengled.media.server.rtsp.RtspMediaSink;
import com.sengled.media.server.rtsp.rtcp.InterleavedRtcpPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author las
 * @date 18-10-9
 */
public class InterleavedRtcpHandler extends SimpleChannelInboundHandler<InterleavedRtcpPacket> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterleavedRtcpHandler.class);

    private final RtspMediaSink rtspMediaSink;

    public InterleavedRtcpHandler(RtspMediaSink rtspMediaSink) {
        super(false);
        this.rtspMediaSink = rtspMediaSink;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, InterleavedRtcpPacket msg) throws Exception {
        rtspMediaSink.onRtcp(msg);
    }
}
