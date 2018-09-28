package com.sengled.media.server.rtsp.rtp.codec;

import com.sengled.media.server.rtsp.rtp.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class InterleavedRtpDecoder extends MessageToMessageDecoder<FramingRtpPacket> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterleavedRtpDecoder.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("{}", cause.getMessage(), cause);
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
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, FramingRtpPacket msg, List<Object> out) throws Exception {
        final int channel = msg.getChannel();
        ByteBuf data = msg.content().duplicate();
        if (channel % 2 == 0) {
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

            RtpHeader header = new FixedRtpHeader(version, padding, extension, cc, marker, payloadType, seqNo, time, ssrc, csrc, profile, extensionLength, headerExtension);

            RtpPayload payload = new RtpPayload(data.readSlice(data.readableBytes()).retain());

            out.add(new DefaultInterleavedRtpPacket(msg.getChannel(), header, payload));
        } else {
            //rtcp
            ctx.fireChannelRead(msg.retain());
        }
    }

}
