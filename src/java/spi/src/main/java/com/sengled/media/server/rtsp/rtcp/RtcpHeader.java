package com.sengled.media.server.rtsp.rtcp;

import io.netty.buffer.ByteBuf;

/**
 * @author las
 * @date 18-10-8
 */
public class RtcpHeader {

    /*
     * RTPC Packet Types
     */
    public static final int RTCP_SR = 200;
    public static final int RTCP_RR = 201;
    public static final int RTCP_SDES = 202;
    public static final int RTCP_BYE = 203;
    public static final int RTCP_APP = 204;

    /**
     * protocol version
     */
    protected int version;

    /**
     * padding flag
     */
    protected boolean padding;

    /**
     * varies by packet type
     */
    protected int count;

    /**
     * RTCP packet type
     */
    protected int packetType;

    /**
     * Packet length in words, w/o this word
     */
    protected int length;

    protected RtcpHeader() {
        this(false, 0);
    }

    public RtcpHeader(boolean padding, int pt) {
        this.padding = padding;
        this.packetType = pt;
        this.count = 0;
        this.length = 0;
        this.version = 2;
    }

    protected void decode(ByteBuf byteBuf) {
        int b = byteBuf.readUnsignedByte();

        this.version = (b & 0xC0) >> 6;
        this.padding = (b & 0x20) == 0x020;

        this.count = b & 0x1F;

        this.packetType = byteBuf.readUnsignedByte();

        this.length = byteBuf.readUnsignedShort();

        /**
         * The length of this RTCP packet in 32-bit words minus one, including
         * the header and any padding. (The offset of one makes zero a valid
         * length and avoids a possible infinite loop in scanning a compound
         * RTCP packet, while counting 32-bit words avoids a validity check for
         * a multiple of 4.)
         */
        this.length = (this.length * 4) + 4;
    }


    protected void encode(ByteBuf byteBuf) {
        byte b = (byte) (this.version << 6);
        if (this.padding) {
            b |= 0x20;
        }

        b |= (byte) (this.count & 0x1F);

        byteBuf.writeByte(b);

        byteBuf.writeByte(this.packetType);

        // Setting length is onus of concrete class. But we increment the offSet
        byteBuf.writeZero(2);
        
    }

    public int getVersion() {
        return version;
    }

    public boolean isPadding() {
        return padding;
    }

    public int getCount() {
        return count;
    }

    public int getPacketType() {
        return packetType;
    }

    public int getLength() {
        return length;
    }

}
