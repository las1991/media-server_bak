package com.sengled.media.server.rtsp.servlet;

import org.apache.commons.io.IOUtils;
import org.mobicents.media.server.impl.rtcp.RtcpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.MediaSink;
import com.sengled.media.MediaSource;
import com.sengled.media.StreamContext;
import com.sengled.media.server.MutableFramePacket;
import com.sengled.media.server.StreamingMediaSink;
import com.sengled.media.server.rtsp.InterleavedFrame;
import com.sengled.media.server.rtsp.Transport;
import com.sengled.media.server.rtsp.rtp.RTP;
import com.sengled.media.server.rtsp.rtp.RtpPacketI;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpPacketizer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.RecyclableArrayList;

/**
 * 通过 RTSP over TCP
 *
 * @author chenxh
 */
public class RtspOverTcpSink extends StreamingMediaSink implements MediaSink {
    private static final Logger LOGGER = LoggerFactory.getLogger(RtspOverTcpSink.class);

    private final RtpPacketizer<?>[] rtpPacketizers;

    public RtspOverTcpSink(ChannelHandlerContext channel, MediaSource source, RtpPacketizer<?>[] rtpPacketizers) {
        super(channel, source, rtpPacketizers);
        this.rtpPacketizers = rtpPacketizers;
    }

    private boolean isOK(RtpPacketizer<?> rtpStream) {
        return null != rtpStream && rtpStream.isOK();
    }

    @Override
    protected void writeInterleaved(StreamContext<?> ctx, MutableFramePacket frame) {
        writeInterleaved0((RtpPacketizer<?>) ctx, frame);
        channel().flush();
    }

    private void writeInterleaved0(RtpPacketizer<?> packetizer, MutableFramePacket frame) {
        RecyclableArrayList out = newArrayList();
        try {
            if (isOK(packetizer)) { // 打包，输出
                packetizer.packet(frame.retain(), out);
                while (!out.isEmpty()) {
                    final Object item = out.remove(0);
                    if (item instanceof RtpPacketI) {
                        RtpPacketI rtp = (RtpPacketI) item;

                        write(new InterleavedFrame(packetizer.getRtpChannel(), RTP.encode(rtp)));
                    } else if (item instanceof RtcpPacket) {
                        RtcpPacket rtcp = (RtcpPacket) item;
                        ByteBuf rtcpBytes = RTP.encode(rtcp);
                        write(new InterleavedFrame(packetizer.getRtcpChannel(), rtcpBytes));

                        LOGGER.debug("stream#{} time = {}", packetizer.getCodec(), rtcp);
                    } else {
                        ReferenceCountUtil.release(item);
                        throw new UnsupportedMessageTypeException(item.getClass());
                    }
                }
            }
        } finally {
            recycle(out);
        }
    }

    @Override
    protected void beforeClose0() {
        // 发送 BYE
        for (RtpPacketizer<?> rtpStream : rtpPacketizers) {
            try {
                if (!isOK(rtpStream) || !isWritable()) {
                    continue;
                }
    
                final int rtcpChannel = rtpStream.getRtcpChannel();
                final ByteBuf rtcp = RTP.encode(rtpStream.bye());
    
                // 发送 RTCP BYE
                writeAndFlush(new InterleavedFrame(rtcpChannel, rtcp)).addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        LOGGER.info("[{}] sent BYE success ? {}", getToken(), future.isSuccess());
                    }
                });
            } finally {
               IOUtils.closeQuietly(rtpStream);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{").append(getClass().getSimpleName());
        buf.append(", src=").append(getToken());
        //buf.append(", userAgent=").append(session.getUserAgent());
        buf.append(", remoteAddress=").append(remoteAddress());
        buf.append("}");
        return buf.toString();
    }

}
