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
        this.sdesChunks = new ArrayList<RtcpSdesChunk>(RtcpPacket.MAX_SOURCES);
    }

    public RtcpSdes(boolean padding) {
        super(padding, RtcpHeader.RTCP_SDES);
        this.sdesChunks = new ArrayList<RtcpSdesChunk>(RtcpPacket.MAX_SOURCES);
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
    protected int encode(byte[] rawData, int offSet) {
        int startPosition = offSet;

        offSet = super.encode(rawData, offSet);
        for (RtcpSdesChunk rtcpSdesChunk : sdesChunks) {
            if (rtcpSdesChunk != null) {
                offSet = rtcpSdesChunk.encode(rawData, offSet);
            } else {
                break;
            }
        }

        /* Reduce 4 octest of header and length is in terms 32bits word */
        this.length = (offSet - startPosition - 4) / 4;

        rawData[startPosition + 2] = ((byte) ((this.length & 0xFF00) >> 8));
        rawData[startPosition + 3] = ((byte) (this.length & 0x00FF));

        return offSet;
    }

    public void addRtcpSdesChunk(RtcpSdesChunk rtcpSdesChunk) {
        if (this.count >= RtcpPacket.MAX_SOURCES) {
            throw new ArrayIndexOutOfBoundsException("Reached maximum number of chunks: " + RtcpPacket.MAX_SOURCES);
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
}
