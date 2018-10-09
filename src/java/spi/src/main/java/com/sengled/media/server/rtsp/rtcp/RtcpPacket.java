package com.sengled.media.server.rtsp.rtcp;

import io.netty.buffer.ByteBuf;

/**
 * @author las
 * @date 18-10-9
 */
public interface RtcpPacket {

    void decode(ByteBuf byteBuf);

    void encode(ByteBuf byteBuf);

    boolean isSender();

    RtcpPacketType getPacketType();

    RtcpReport getReport();

    RtcpSenderReport getSenderReport();

    RtcpReceiverReport getReceiverReport();

    RtcpSdes getSdes();

    RtcpBye getBye();

    boolean hasBye();

    RtcpAppDefined getAppDefined();

    int getPacketCount();

    int getSize();
}
