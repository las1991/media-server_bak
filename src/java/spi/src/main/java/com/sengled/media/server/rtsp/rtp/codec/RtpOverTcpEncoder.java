package com.sengled.media.server.rtsp.rtp.codec;

import com.sengled.media.server.rtsp.InterleavedFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author las
 * @date 18-9-26
 */
public class RtpOverTcpEncoder extends MessageToByteEncoder<InterleavedFrame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, InterleavedFrame src, ByteBuf out) {
        outputFrame(src, out);
    }

    private void outputFrame(InterleavedFrame frame, ByteBuf out) {
        out.writeByte('$');
        out.writeByte(frame.channel());
        out.writeShort(frame.content().readableBytes());
        out.writeBytes(frame.content());
    }
}
