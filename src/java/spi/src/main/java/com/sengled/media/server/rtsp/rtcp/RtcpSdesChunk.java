package com.sengled.media.server.rtsp.rtcp;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * @author las
 * @date 18-10-8
 */
public class RtcpSdesChunk {

    public static final int MAX_ITEMS = 9;

    private long ssrc;

    private final List<RtcpSdesItem> rtcpSdesItems;

    private int itemCount = 0;

    public RtcpSdesChunk(long ssrc) {
        this.ssrc = ssrc;
        this.rtcpSdesItems = new ArrayList<RtcpSdesItem>(MAX_ITEMS);
    }

    protected RtcpSdesChunk() {
        this.rtcpSdesItems = new ArrayList<RtcpSdesItem>(MAX_ITEMS);
    }

    protected void decode(ByteBuf byteBuf) {

        this.ssrc = byteBuf.readUnsignedInt();

        while (true) {
            RtcpSdesItem sdesItem = new RtcpSdesItem();
            sdesItem.decode(byteBuf);
            addRtcpSdesItem(sdesItem);

            if (RtcpSdesItem.RTCP_SDES_END == sdesItem.getType()) {
                break;
            }
        }

    }

    public void addRtcpSdesItem(RtcpSdesItem rtcpSdesItem) {
        if (this.itemCount >= MAX_ITEMS) {
            throw new ArrayIndexOutOfBoundsException("Reached maximum number of items: " + MAX_ITEMS);
        }
        this.rtcpSdesItems.add(rtcpSdesItem);
        this.itemCount++;
    }

    public long getSsrc() {
        return ssrc;
    }

    public String getCname() {
        for (RtcpSdesItem item : this.rtcpSdesItems) {
            if (RtcpSdesItem.RTCP_SDES_CNAME == item.getType()) {
                return item.getText();
            }
        }
        return "";
    }

    public RtcpSdesItem[] getRtcpSdesItems() {
        RtcpSdesItem[] items = new RtcpSdesItem[this.rtcpSdesItems.size()];
        return rtcpSdesItems.toArray(items);
    }

    public int getItemCount() {
        return itemCount;
    }

    protected int encode(byte[] rawData, int offSet) {

        int temp = offSet;

        rawData[offSet++] = ((byte) ((this.ssrc & 0xFF000000) >> 24));
        rawData[offSet++] = ((byte) ((this.ssrc & 0x00FF0000) >> 16));
        rawData[offSet++] = ((byte) ((this.ssrc & 0x0000FF00) >> 8));
        rawData[offSet++] = ((byte) ((this.ssrc & 0x000000FF)));

        for (RtcpSdesItem rtcpSdesItem : rtcpSdesItems) {
            if (rtcpSdesItem != null) {
                offSet = rtcpSdesItem.encode(rawData, offSet);
            } else {
                break;
            }
        }

        // This is End
        rawData[offSet++] = 0x00;

        int remainder = (offSet - temp) % 4;
        if (remainder != 0) {
            int pad = 4 - remainder;
            for (int i = 0; i < pad; i++) {
                rawData[offSet++] = 0x00;
            }
        }

        return offSet;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("SDES CHUNK:\n");
        builder.append("ssrc= ").append(this.ssrc).append(", ");
        builder.append("item count= ").append(this.itemCount);
        for (RtcpSdesItem item : this.rtcpSdesItems) {
            builder.append("\n").append(item.toString());
        }
        return builder.toString();
    }
}
