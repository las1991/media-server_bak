package com.sengled.media.server.rtsp.rtcp;

import io.netty.buffer.ByteBuf;

/**
 * @author las
 * @date 18-10-8
 */
public class RtcpReceiverReport extends RtcpReport {

    private static final boolean IS_SENDER = false;

    protected RtcpReceiverReport() {
        super();
    }

    public RtcpReceiverReport(boolean padding, long ssrc) {
        super(padding, ssrc, RtcpHeader.RTCP_RR);
    }

    @Override
    protected void decode(ByteBuf byteBuf) {

        int readerIndex = byteBuf.readerIndex();

        super.decode(byteBuf);

        this.ssrc = byteBuf.readUnsignedInt();

        while ((byteBuf.readerIndex() - readerIndex) < this.length) {
            RtcpReportBlock rtcpReceptionReportItem = new RtcpReportBlock();
            rtcpReceptionReportItem.decode(byteBuf);
            this.reportBlocks.add(rtcpReceptionReportItem);
        }

    }

    @Override
    protected int encode(byte[] rawData, int offSet) {
        int startPosition = offSet;

        offSet = super.encode(rawData, offSet);

        rawData[offSet++] = ((byte) ((this.ssrc & 0xFF000000) >> 24));
        rawData[offSet++] = ((byte) ((this.ssrc & 0x00FF0000) >> 16));
        rawData[offSet++] = ((byte) ((this.ssrc & 0x0000FF00) >> 8));
        rawData[offSet++] = ((byte) ((this.ssrc & 0x000000FF)));

        for (RtcpReportBlock report : this.reportBlocks) {
            if (report != null) {
                offSet = report.encode(rawData, offSet);
            } else {
                break;
            }
        }

        /* Reduce 4 octets of header and length is in terms 32bits word */
        this.length = (offSet - startPosition - 4) / 4;

        rawData[startPosition + 2] = ((byte) ((this.length & 0xFF00) >> 8));
        rawData[startPosition + 3] = ((byte) (this.length & 0x00FF));

        return offSet;
    }

    @Override
    public boolean isSender() {
        return IS_SENDER;
    }
}
