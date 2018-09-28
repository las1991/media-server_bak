package com.sengled.media.server.rtsp.rtp.codec;

import com.sengled.media.server.rtsp.rtp.FramingRtpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author las
 * @date 18-9-28
 */
public class RtpOverTcpDecoder extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpOverTcpDecoder.class);

    public static final byte DOLLAR = 0x24;

    enum STATE {
        READ_FIRST_BYTE,
        READ_RTP_CHANNEL,
        READ_RTP_LENGTH,
        READ_RTP
    }

    private STATE state = STATE.READ_FIRST_BYTE;

    private int channel;
    private int length;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("{}", cause.getMessage(), cause);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state) {
            case READ_FIRST_BYTE:
                byte ch = in.readByte();
                if (ch != DOLLAR) {
                    in.readerIndex(in.readerIndex() - 1);
                    // 不是rtp包
                    ctx.fireChannelRead(in.retain());
                    break;
                }
                state = STATE.READ_RTP_CHANNEL;

            case READ_RTP_CHANNEL:
                if (in.readableBytes() < 1) {
                    break;
                }
                channel = in.readUnsignedByte();
                state = STATE.READ_RTP_LENGTH;

            case READ_RTP_LENGTH:
                if (in.readableBytes() < 2) {
                    break;
                }
                length = in.readUnsignedShort();
                state = STATE.READ_RTP;
            case READ_RTP:
                if (in.readableBytes() >= length) {
                    FramingRtpPacket packet = new FramingRtpPacket(channel, length, in.readSlice(length).retain());
                    if (null != packet) {
                        out.add(packet);
                    }
                    state = STATE.READ_FIRST_BYTE;
                }
            default:
        }

    }
}
