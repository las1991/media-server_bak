package com.sengled.media.server.rtsp.rtcp;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author las
 * @date 18-10-8
 */
public class RtcpPacket {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtcpPacket.class);

    /**
     * Maximum number of reporting sources
     */
    public static final int MAX_SOURCES = 31;

    private RtcpSenderReport senderReport = null;
    private RtcpReceiverReport receiverReport = null;
    private RtcpSdes sdes = null;
    private RtcpBye bye = null;
    private RtcpAppDefined appDefined = null;

    private int packetCount = 0;
    private int size = 0;

    public RtcpPacket() {

    }

    public RtcpPacket(RtcpSenderReport senderReport, RtcpReceiverReport receiverReport, RtcpSdes sdes, RtcpBye bye, RtcpAppDefined appDefined) {
        this.senderReport = senderReport;
        this.receiverReport = receiverReport;
        this.sdes = sdes;
        this.bye = bye;
        this.appDefined = appDefined;
    }

    public RtcpPacket(RtcpReport report, RtcpSdes sdes, RtcpBye bye) {
        if (report.isSender()) {
            this.senderReport = (RtcpSenderReport) report;
        } else {
            this.receiverReport = (RtcpReceiverReport) report;
        }
        this.sdes = sdes;
        this.bye = bye;
    }

    public RtcpPacket(RtcpReport report, RtcpSdes sdes) {
        this(report, sdes, null);
    }

    public void decode(ByteBuf byteBuf) {
//        this.size = byteBuf.readableBytes();
        this.size = 0;
        while (byteBuf.readableBytes() > 0) {
            int type = byteBuf.getUnsignedByte(byteBuf.readerIndex());
            switch (type) {
                case RtcpHeader.RTCP_SR:
                    packetCount++;
                    this.senderReport = new RtcpSenderReport();
                    this.senderReport.decode(byteBuf);
                    this.size += this.senderReport.length;
                    break;
                case RtcpHeader.RTCP_RR:
                    packetCount++;
                    this.receiverReport = new RtcpReceiverReport();
                    this.receiverReport.decode(byteBuf);
                    this.size += this.receiverReport.length;
                    break;
                case RtcpHeader.RTCP_SDES:
                    packetCount++;
                    this.sdes = new RtcpSdes();
                    this.sdes.decode(byteBuf);
                    this.size += this.sdes.length;
                    break;
                case RtcpHeader.RTCP_APP:
                    packetCount++;
                    this.appDefined = new RtcpAppDefined();
                    this.appDefined.decode(byteBuf);
                    this.size += this.appDefined.length;
                    break;
                case RtcpHeader.RTCP_BYE:
                    packetCount++;
                    this.bye = new RtcpBye();
                    this.bye.decode(byteBuf);
                    this.size += this.bye.length;
                    break;
                default:
                    LOGGER.error("Received type = " + type + " RTCP Packet decoding falsed. offSet = " + byteBuf.readerIndex() + ". Packet count = " + packetCount);
                    byteBuf.readerIndex(byteBuf.writerIndex());
                    break;
            }
        }

    }

    public void encode(ByteBuf byteBuf) {
        int initalOffSet = byteBuf.writerIndex();
        if (this.senderReport != null) {
            packetCount++;
            this.senderReport.encode(byteBuf);
        }
        if (this.receiverReport != null) {
            packetCount++;
            this.receiverReport.encode(byteBuf);
        }
        if (this.sdes != null) {
            packetCount++;
            this.sdes.encode(byteBuf);
        }
        if (this.appDefined != null) {
            packetCount++;
            this.appDefined.encode(byteBuf);
        }
        if (this.bye != null) {
            packetCount++;
            this.bye.encode(byteBuf);
        }
        this.size = byteBuf.writerIndex() - initalOffSet;
    }

    public boolean isSender() {
        return this.senderReport != null;
    }

    public RtcpPacketType getPacketType() {
        if(this.bye == null) {
            return RtcpPacketType.RTCP_REPORT;
        }
        return RtcpPacketType.RTCP_BYE;
    }

    public RtcpReport getReport() {
        if (isSender()) {
            return this.senderReport;
        }
        return this.receiverReport;
    }

    public RtcpSenderReport getSenderReport() {
        return senderReport;
    }

    public RtcpReceiverReport getReceiverReport() {
        return receiverReport;
    }

    public RtcpSdes getSdes() {
        return sdes;
    }

    public RtcpBye getBye() {
        return bye;
    }

    public boolean hasBye() {
        return this.bye != null;
    }

    public RtcpAppDefined getAppDefined() {
        return appDefined;
    }

    public int getPacketCount() {
        return packetCount;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        // Print RR/SR
        RtcpReport report = getReport();
        if (report != null) {
            builder.append(report.toString());
        }
        // Print SDES if exists
        if (this.sdes != null) {
            builder.append(this.sdes.toString());
        }
        // Print BYE if exists
        if (this.bye != null) {
            builder.append(bye.toString());
        }

        return builder.toString();
    }
}
