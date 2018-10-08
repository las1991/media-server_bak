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
    protected void encode(ByteBuf byteBuf) {
        int startPosition = byteBuf.writerIndex();

        super.encode(byteBuf);

        int lengthIndex = byteBuf.writerIndex() - 2;

        byteBuf.writeInt((int) this.ssrc);

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

    @Override
    public boolean isSender() {
        return IS_SENDER;
    }
}
