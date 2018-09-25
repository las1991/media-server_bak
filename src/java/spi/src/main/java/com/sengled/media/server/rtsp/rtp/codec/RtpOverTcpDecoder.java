package com.sengled.media.server.rtsp.rtp.codec;

import com.sengled.media.server.rtsp.IllegalFrame;
import com.sengled.media.server.rtsp.rtp.RtpOverTcpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RtpOverTcpDecoder extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpOverTcpDecoder.class);

    public static final byte DOLLAR = 0x24;

    enum STATE {
        READ_FIRST_BYTE,
        READ_RTP_CHANNEL,
        READ_RTP_LENGTH,
        READ_RTP,
        READ_RTCP
    }

    private STATE state = STATE.READ_FIRST_BYTE;

    private short channel;
    private int length;


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() > 0 && state == STATE.READ_FIRST_BYTE) {
            switch (state) {
                case READ_FIRST_BYTE:
                    byte ch = in.readByte();
                    if (ch != DOLLAR) {
                        in.readerIndex(in.readerIndex() - 1);
                        // 非法的数据
                        out.add(new IllegalFrame(in.readSlice(in.readableBytes()).retain()));
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
                    if (channel % 2 == 0) {
                        state = STATE.READ_RTP;
                    } else {
                        state = STATE.READ_RTCP;
                    }

                case READ_RTP:
                    if (in.readableBytes() >= length) {
                        RtpOverTcpPacket packet = decode0(in.readSlice(length).retain());
                        if (null != packet) {
                            out.add(packet);
                        }
                        state = STATE.READ_FIRST_BYTE;
                    }
                    break;
                case READ_RTCP:
                    //TODO 解码rtcp

                    ByteBuf rtcp = in.readSlice(length);

                    LOGGER.info("read rtcp packet {}", ByteBufUtil.hexDump(rtcp));
                    rtcp.release();
                    break;

                default:
            }
        }

    }

    /**
     * The RTP header has the following format:
     * <p>
     * 0                   1                   2                   3
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |V=2|P|X|  CC   |M|     PT      |       sequence number         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                           timestamp                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |           synchronization source (SSRC) identifier            |
     * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
     * |            contributing source (CSRC) identifiers             |
     * |                             ....                              |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * <p>
     * The first twelve octets are present in every RTP packet, while the
     * list of CSRC identifiers is present only when inserted by a mixer.
     * <p>
     * The version defined by RFC3550 specification is two.
     *
     * @param data
     * @return
     * @author las
     */
    public RtpOverTcpPacket decode0(ByteBuf data) {
        try {
            int byte0, byte1, byte2, byte3;
            byte0 = data.readUnsignedByte();
            byte1 = data.readUnsignedByte();
            byte2 = data.readUnsignedByte();
            byte3 = data.readUnsignedByte();

            final int version = (byte0 & 0xC0) >> 6;
            final boolean padding = (byte0 & 0x20) > 0;
            final boolean extension = (byte0 & 0x10) > 0;
            final int cc = (byte0 & 0x0F);

            final boolean marker = (byte1 & 0x80) > 0;
            final int payloadType = (byte1 & 0x7F);
            final int seqNo = (byte2 << 8) | (byte3 << 0);

            final long time = data.readUnsignedInt();
            final long ssrc = data.readUnsignedInt();

            ArrayList<Long> csrc = new ArrayList<>(cc + 1);
            for (int i = 0; i < cc; i++) {
                csrc.add(data.readUnsignedInt());
            }

            int profile = 0;
            int extensionLength = 0;
            byte[] headerExtension = null;
            if (extension) {
                profile = data.readUnsignedShort();
                extensionLength = data.readUnsignedShort();

                headerExtension = new byte[extensionLength * 4];
                data.readBytes(headerExtension);
            }

            return new RtpOverTcpPacket(channel, data.readSlice(data.readableBytes()).retain(), version, padding, extension, cc, marker, payloadType, seqNo, time, ssrc, csrc, profile, extensionLength, headerExtension);
        } catch (Exception e) {
            LOGGER.error("fial with {}", e.getMessage(), e);
        } finally {
            data.release();
        }

        return null;
    }

}
