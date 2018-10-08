package com.sengled.media.server.rtsp.rtcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;

/**
 * @author las
 * @date 18-10-8
 */
public class RtcpSdesItem {

    public static final short RTCP_SDES_END = 0;
    public static final short RTCP_SDES_CNAME = 1;
    public static final short RTCP_SDES_NAME = 2;
    public static final short RTCP_SDES_EMAIL = 3;
    public static final short RTCP_SDES_PHONE = 4;
    public static final short RTCP_SDES_LOC = 5;
    public static final short RTCP_SDES_TOOL = 6;
    public static final short RTCP_SDES_NOTE = 7;
    public static final short RTCP_SDES_PRIV = 8;

    /*
     * SDES item
     */

    /* type of item (rtcp_sdes_type_t) */
    private int type = 0;

    /* length of item (in octets) */
    private int length = 0;

    /* text, not null-terminated */
    private String text = null;

    public RtcpSdesItem(short type, String text) {
        this.type = type;
        this.text = text;
    }

    protected RtcpSdesItem() {

    }

    protected void decode(ByteBuf byteBuf) {
        this.type = byteBuf.readUnsignedByte();

        if (type == RtcpSdesItem.RTCP_SDES_END) {
            int index = byteBuf.forEachByte(ByteBufProcessor.FIND_NUL);
            byteBuf.readerIndex(index);
        }

        this.length = byteBuf.readUnsignedByte();

        byte[] chunkData = new byte[length];
        byteBuf.readBytes(chunkData);
        this.text = new String(chunkData);

    }

    protected void encode(ByteBuf byteBuf) {
        byte[] textData = this.text.getBytes();

        byteBuf.writeByte(this.type);
        byteBuf.writeByte(this.length);

        byteBuf.writeBytes(textData, 0, textData.length);
    }

    public int getType() {
        return type;
    }

    public int getLength() {
        return length;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("SDES ITEM: \n");
        builder.append("type= ").append(resolveType(this.type)).append(", ");
        builder.append("value= ").append(this.text).append(", ");
        builder.append("length= ").append(this.length).append("\n");
        return builder.toString();
    }

    private String resolveType(int type) {
        switch (type) {
            case RTCP_SDES_END:
                return "END";
            case RTCP_SDES_CNAME:
                return "CNAME";
            case RTCP_SDES_NAME:
                return "NAME";
            case RTCP_SDES_EMAIL:
                return "EMAIL";
            case RTCP_SDES_PHONE:
                return "PHONE";
            case RTCP_SDES_LOC:
                return "LOC";
            case RTCP_SDES_TOOL:
                return "TOOL";
            case RTCP_SDES_NOTE:
                return "NOTE";
            case RTCP_SDES_PRIV:
                return "PRIV";
            default:
                return "UNKNOWN";
        }
    }
}
