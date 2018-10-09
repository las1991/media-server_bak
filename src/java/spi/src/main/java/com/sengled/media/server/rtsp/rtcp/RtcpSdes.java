package com.sengled.media.server.rtsp.rtcp;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * @author las
 * @date 18-10-8
 */
public class RtcpSdes extends RtcpHeader {

    /**
     * SDES
     */
    private final List<RtcpSdesChunk> sdesChunks;

    protected RtcpSdes() {
        this.sdesChunks = new ArrayList<RtcpSdesChunk>(DefaultRtcpPacket.MAX_SOURCES);
    }

    public RtcpSdes(boolean padding) {
        super(padding, RtcpHeader.RTCP_SDES);
        this.sdesChunks = new ArrayList<RtcpSdesChunk>(DefaultRtcpPacket.MAX_SOURCES);
    }

    @Override
    protected void decode(ByteBuf byteBuf) {
        int readerIndex = byteBuf.readerIndex();

        super.decode(byteBuf);

        while ((byteBuf.readableBytes() - readerIndex) < this.length) {
            RtcpSdesChunk rtcpSdesChunk = new RtcpSdesChunk();
            rtcpSdesChunk.decode(byteBuf);
            this.sdesChunks.add(rtcpSdesChunk);
        }

    }

    @Override
    protected void encode(ByteBuf byteBuf) {
        int startPosition = byteBuf.writerIndex();

        super.encode(byteBuf);
        int lengthIndex = byteBuf.writerIndex() - 2;

        for (RtcpSdesChunk rtcpSdesChunk : sdesChunks) {
            if (rtcpSdesChunk != null) {
                rtcpSdesChunk.encode(byteBuf);
            } else {
                break;
            }
        }

        /* Reduce 4 octest of header and length is in terms 32bits word */
        this.length = (byteBuf.writerIndex() - startPosition - 4) / 4;

        byteBuf.setShort(lengthIndex, this.length);

    }

    public void addRtcpSdesChunk(RtcpSdesChunk rtcpSdesChunk) {
        if (this.count >= DefaultRtcpPacket.MAX_SOURCES) {
            throw new ArrayIndexOutOfBoundsException("Reached maximum number of chunks: " + DefaultRtcpPacket.MAX_SOURCES);
        }
        this.sdesChunks.add(rtcpSdesChunk);
        this.count++;
    }

    public RtcpSdesChunk[] getSdesChunks() {
        RtcpSdesChunk[] chunks = new RtcpSdesChunk[this.sdesChunks.size()];
        return this.sdesChunks.toArray(chunks);
    }

    public String getCname() {
        for (RtcpSdesChunk chunk : this.sdesChunks) {
            String cname = chunk.getCname();
            if (cname != null && !cname.isEmpty()) {
                return cname;
            }
        }
        return "";
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("SDES:\n");
        builder.append("version= ").append(this.version).append(", ");
        builder.append("padding= ").append(this.padding).append(", ");
        builder.append("source count= ").append(this.count).append(", ");
        builder.append("packet type= ").append(this.packetType).append(", ");
        builder.append("length= ").append(this.length).append(", ");
        for (RtcpSdesChunk chunk : this.sdesChunks) {
            builder.append("\n").append(chunk.toString());
        }
        return builder.toString();
    }

}
