package com.sengled.media.server.rtsp.rtcp;

import io.netty.buffer.ByteBuf;

/**
 * @author las
 * @date 18-10-8
 */
public class RtcpReportBlock {
    /*
     * Reception report block
     */

    /* data source being reported */
    private long ssrc = 0;

    /* fraction lost since last SR/RR */
    private int fraction = 0;

    /* cumul. no. pkts lost (signed!) */
    private int lost = 0;

    /* corresponding count of sequence number cycles */
    private int SeqNumCycle;

    /* extended last seq. no. received */
    private long lastSeq = 0;

    /* interarrival jitter */
    private int jitter = 0;

    /* last SR packet from this source */
    private long lsr = 0;

    /* delay since last SR packet */
    private long dlsr = 0;

    protected RtcpReportBlock() {

    }

    public RtcpReportBlock(long ssrc, int fraction, int lost, int SeqNumCycle, long lastSeq, int jitter, long lsr, long dlsr) {
        this.ssrc = ssrc;
        this.fraction = fraction;
        this.lost = lost;
        this.SeqNumCycle = SeqNumCycle;
        this.lastSeq = lastSeq;
        this.jitter = jitter;
        this.lsr = lsr;
        this.dlsr = dlsr;
    }

    public long getSsrc() {
        return ssrc;
    }

    public int getFraction() {
        return fraction;
    }

    public int getLost() {
        return lost;
    }

    public int getSeqNumCycle() {
        return SeqNumCycle;
    }

    public long getLastSeq() {
        return lastSeq;
    }

    public int getJitter() {
        return jitter;
    }

    public long getLsr() {
        return lsr;
    }

    public long getDlsr() {
        return dlsr;
    }

    protected void decode(ByteBuf byteBuf) {

        this.ssrc = byteBuf.readUnsignedInt();

        this.fraction = byteBuf.readByte();

        this.lost = byteBuf.readMedium();

        this.SeqNumCycle = byteBuf.readShort();

        this.lastSeq = byteBuf.readShort();

        this.jitter = byteBuf.readInt();

        this.lsr = byteBuf.readUnsignedInt();

        this.dlsr = byteBuf.readUnsignedInt();

    }


    protected void encode(ByteBuf byteBuf) {

        byteBuf.writeInt((int) this.ssrc);

        byteBuf.writeByte(this.fraction);

        byteBuf.writeMedium(this.lost);

        byteBuf.writeShort(this.SeqNumCycle);
        byteBuf.writeShort((int) this.lastSeq);

        byteBuf.writeInt(this.jitter);

        byteBuf.writeInt((int) this.lsr);

        byteBuf.writeInt((int) this.dlsr);
    }
}
