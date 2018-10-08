package com.sengled.media.server.rtsp.rtcp.codec;

import com.sengled.media.server.rtsp.rtcp.RtcpPacket;
import com.sengled.media.server.rtsp.rtp.FramingRtpPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author las
 * @date 18-9-28
 */
public class InterleavedRtcpDecoder extends MessageToMessageDecoder<FramingRtpPacket> {

    private final static Logger LOGGER = LoggerFactory.getLogger(InterleavedRtcpDecoder.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("{}", cause.getMessage(), cause);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, FramingRtpPacket msg, List<Object> out) throws Exception {
        LOGGER.debug("rtcp {}", msg);
        RtcpPacket rtcpPacket = new RtcpPacket();
        rtcpPacket.decode(msg.content().duplicate());
        out.add(rtcpPacket);
    }
}
