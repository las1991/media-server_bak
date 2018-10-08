package com.sengled.media.server.rtsp.rtcp;

import java.util.ArrayList;
import java.util.List;

/**
 * @author las
 * @date 18-10-8
 */
public abstract class RtcpReport extends RtcpHeader {
    /**
     * Source that generated the report
     */
    protected long ssrc;

    /**
     * Reports coming from other sync sources
     */
    protected List<RtcpReportBlock> reportBlocks;

    protected RtcpReport() {
        this.reportBlocks = new ArrayList<RtcpReportBlock>(RtcpPacket.MAX_SOURCES);
    }

    protected RtcpReport(boolean padding, long ssrc, int packetType) {
        super(padding, packetType);
        this.ssrc = ssrc;
        this.reportBlocks = new ArrayList<RtcpReportBlock>(RtcpPacket.MAX_SOURCES);
    }

    /**
     * Tells whether this reports was generated by a sender or a receiver.
     *
     * @return Whether this is a Sender Report or not.
     */
    public abstract boolean isSender();

    public long getSsrc() {
        return this.ssrc;
    }

    public List<RtcpReportBlock> getReportBlocks() {
        return this.reportBlocks;
    }

    public RtcpReportBlock getReportBlock(long ssrc) {
        for (RtcpReportBlock report : this.reportBlocks) {
            if (report.getSsrc() == ssrc) {
                return report;
            }
        }
        return null;
    }

    public void addReceiverReport(RtcpReportBlock rtcpReceptionReportItem) {
        if (this.count >= RtcpPacket.MAX_SOURCES) {
            throw new ArrayIndexOutOfBoundsException("Reached maximum number of items: " + RtcpPacket.MAX_SOURCES);
        }
        this.reportBlocks.add(rtcpReceptionReportItem);
        this.count++;
    }
}
