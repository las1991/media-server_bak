package com.sengled.media.server.rtsp.rtcp;

import com.sengled.media.server.rtsp.rtcp.ntp.NtpUtils;
import io.netty.buffer.ByteBuf;

/**
 * @author las
 * @date 18-10-8
 */
public class RtcpSenderReport extends RtcpReport {

    private static final boolean IS_SENDER = true;

    /**
     * NTP timestamp
     */
    private long ntpSec;

    /**
     * NTP fraction
     */
    private long ntpFrac;

    /**
     * The middle 32 bits out of 64 in the NTP timestamp
     */
    private long ntpTs;

    /**
     * RTP timestamp
     */
    private long rtpTs;

    /**
     * packets sent
     */
    private long psent;

    /**
     * octets sent
     */
    private long osent;

    protected RtcpSenderReport() {
        super();
    }

    public RtcpSenderReport(boolean padding, long ssrc, long ntpSec, long ntpFrac, long rtpTs, long psent, long osent) {
        super(padding, ssrc, RtcpHeader.RTCP_SR);
        this.ntpSec = ntpSec;
        this.ntpFrac = ntpFrac;
        this.ntpTs = NtpUtils.calculateLastSrTimestamp(ntpSec, ntpFrac);
        this.rtpTs = rtpTs;
        this.psent = psent;
        this.osent = osent;
    }

    @Override
    protected void decode(ByteBuf byteBuf) {

        int readerIndex = byteBuf.readerIndex();

        super.decode(byteBuf);

        this.ssrc = byteBuf.readUnsignedInt();

        this.ntpSec = byteBuf.readUnsignedInt();

        this.ntpFrac = byteBuf.readUnsignedInt();

        this.ntpTs = NtpUtils.calculateLastSrTimestamp(ntpSec, ntpFrac);

        this.rtpTs = byteBuf.readUnsignedInt();

        this.psent = byteBuf.readUnsignedInt();

        this.osent = byteBuf.readUnsignedInt();

        while ((byteBuf.readerIndex() - readerIndex) < this.length) {
            RtcpReportBlock rtcpReceptionReportItem = new RtcpReportBlock();
            rtcpReceptionReportItem.decode(byteBuf);
            addReceiverReport(rtcpReceptionReportItem);
        }
    }

    @Override
    protected void encode(ByteBuf byteBuf) {
        int startPosition = byteBuf.writerIndex();

        super.encode(byteBuf);
        int lengthIndex = byteBuf.writerIndex() - 2;

        byteBuf.writeInt((int) this.ssrc);

        byteBuf.writeInt((int) this.ntpSec);

        byteBuf.writeInt((int) this.ntpFrac);

        byteBuf.writeInt((int) this.rtpTs);

        byteBuf.writeInt((int) this.psent);

        byteBuf.writeInt((int) this.osent);

        for (RtcpReportBlock report : this.reportBlocks) {
            if (report != null) {
                report.encode(byteBuf);
            } else {
                break;
            }
        }

        /* Reduce 4 octets of header and length is in terms 32bits word */
        this.length = (byteBuf.writerIndex() - startPosition - 4) / 4;

        byteBuf.setShort(lengthIndex, this.length);

    }

    public long getNtpSec() {
        return ntpSec;
    }

    public long getNtpFrac() {
        return ntpFrac;
    }

    public long getNtpTs() {
        return ntpTs;
    }

    public long getRtpTs() {
        return rtpTs;
    }

    public long getPsent() {
        return psent;
    }

    public long getOsent() {
        return osent;
    }

    @Override
    public boolean isSender() {
        return IS_SENDER;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("SENDER REPORT:\n");
        builder.append("version=").append(this.version).append(", ");
        builder.append("padding=").append(this.padding).append(", ");
        builder.append("packet type=").append(this.packetType).append(", ");
        builder.append("length=").append(this.length).append(", ");
        builder.append("ssrc=").append(this.ssrc).append(", ");
        builder.append("ntp seconds=").append(this.ntpSec).append(", ");
        builder.append("ntp fraction=").append(this.ntpFrac).append(", ");
        builder.append("rtp timestamp=").append(this.rtpTs).append(", ");
        builder.append("packets sent=").append(this.psent).append(", ");
        builder.append("octets sent=").append(this.osent).append("\n");
        for (RtcpReportBlock rr : this.reportBlocks) {
            builder.append("\n").append(rr.toString());
        }
        return builder.toString();
    }
}
