/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package com.sengled.media.server.rtsp.rtcp;

import com.sengled.media.server.rtsp.rtp.statistics.RtpMember;
import com.sengled.media.server.rtsp.rtp.statistics.RtpStatistics;
import org.apache.commons.net.ntp.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * Factory for building RTCP packets
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RtcpPacketFactory {

    public static final Logger logger = LoggerFactory.getLogger(RtcpPacketFactory.class);

    /**
     * Builds a packet containing an RTCP Sender Report.
     *
     * @param statistics The statistics of the RTP session
     * @return The RTCP packet
     */
    public static RtcpSenderReport buildSenderReport(RtpStatistics statistics, boolean padding) {
        /*
         *         0                   1                   2                   3
         *         0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         * header |V=2|P|    RC   |   PT=SR=200   |             length            |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                         SSRC of sender                        |
         *        +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
         * sender |              NTP timestamp, most significant word             |
         * info   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |             NTP timestamp, least significant word             |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                         RTP timestamp                         |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                     sender's packet count                     |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                      sender's octet count                     |
         *        +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
         * report |                 SSRC_1 (SSRC of first source)                 |
         * block  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *   1    | fraction lost |       cumulative number of packets lost       |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |           extended highest sequence number received           |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                      interarrival jitter                      |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                         last SR (LSR)                         |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                   delay since last SR (DLSR)                  |
         *        +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
         * report |                 SSRC_2 (SSRC of second source)                |
         * block  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *   2    :                               ...                             :
         *        +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
         *        |                  profile-specific extensions                  |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         */
        long ssrc = statistics.getSsrc();
        long currentTime = statistics.getCurrentTime();
        TimeStamp ntpTs = new TimeStamp(new Date(currentTime));
        long ntpSec = ntpTs.getSeconds();
        long ntpFrac = ntpTs.getFraction();
        long elapsedTime = statistics.getCurrentTime() - statistics.getRtpSentOn();
        long rtpTs = statistics.getRtpTimestamp() + statistics.getRtpTime(elapsedTime);
        long psent = statistics.getRtpPacketsSent();
        long osent = statistics.getRtpOctetsSent();

        RtcpSenderReport senderReport = new RtcpSenderReport(padding, ssrc, ntpSec, ntpFrac, rtpTs, psent, osent);

        // Add receiver reports for each registered member
        List<Long> members = statistics.getMembersList();
        for (Long memberSsrc : members) {
            if (ssrc != memberSsrc) {
                RtpMember memberStats = statistics.getMember(memberSsrc.longValue());
                RtcpReportBlock rcvrReport = buildSubReceiverReport(memberStats);
                senderReport.addReceiverReport(rcvrReport);
            }
        }
        return senderReport;
    }

    /**
     * Builds a packet containing an RTCP Receiver Report
     *
     * @param statistics The statistics of the RTP session
     * @return The RTCP packet
     */
    private static RtcpReceiverReport buildReceiverReport(RtpStatistics statistics, boolean padding) {
        RtcpReceiverReport report = new RtcpReceiverReport(padding, statistics.getSsrc());
        long ssrc = statistics.getSsrc();

        // Add receiver reports for each registered member
        List<Long> members = statistics.getMembersList();
        for (Long memberSsrc : members) {
            if (ssrc != memberSsrc) {
                RtpMember memberStats = statistics.getMember(memberSsrc.longValue());
                RtcpReportBlock rcvrReport = buildSubReceiverReport(memberStats);
                if (rcvrReport.getJitter() >= 0 && rcvrReport.getLost() >= 0) {
                    report.addReceiverReport(rcvrReport);
                }
            }
        }
        return report;
    }

    public static RtcpSdes buildSdes(RtpStatistics statistics, boolean padding) {
        RtcpSdes sdes = new RtcpSdes(padding);
        RtcpSdesChunk chunk = new RtcpSdesChunk(statistics.getSsrc());
        RtcpSdesItem cname = new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, statistics.getCname());
//		RtcpSdesItem end = new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_END, null);
        chunk.addRtcpSdesItem(cname);
//		chunk.addRtcpSdesItem(end);
        sdes.addRtcpSdesChunk(chunk);

        return sdes;
    }

    private static RtcpReportBlock buildSubReceiverReport(RtpMember statistics) {
        long ssrc = statistics.getSsrc();
        int fraction = (int) statistics.getFractionLost();
        int lost = (int) statistics.getPacketsLost();
        int seqNumCycle = statistics.getSequenceCycle();
        long lastSeq = statistics.getExtHighSequence();
        int jitter = (int) statistics.getJitter();
        long lsr = statistics.getLastSR();
        long dlsr = statistics.getLastSRdelay();

        return new RtcpReportBlock(ssrc, fraction, lost, seqNumCycle, lastSeq, jitter, lsr, dlsr);
    }

    /**
     * Builds a packet containing an RTCP Report.
     * <p>
     * RTP receivers provide reception quality feedback using RTCP report
     * packets which may take one of two forms depending upon whether or not the
     * receiver is also a sender. The only difference between the sender report
     * (SR) and receiver report (RR) forms, besides the packet type code, is
     * that the sender report includes a 20-byte sender information section for
     * use by active senders. The SR is issued if a site has sent any data
     * packets during the interval since issuing the last report or the previous
     * one, otherwise the RR is issued.
     * <p>
     * Both the SR and RR forms include zero or more reception report blocks,
     * one for each of the synchronization sources from which this receiver has
     * received RTP data packets since the last report. Reports are not issued
     * for contributing sources listed in the CSRC list. Each reception report
     * block provides statistics about the data received from the particular
     * source indicated in that block.
     * <p>
     * Since a maximum of 31 reception report blocks will fit in an SR or RR
     * packet, additional RR packets SHOULD be stacked after the initial SR or
     * RR packet as needed to contain the reception reports for all sources
     * heard during the interval since the last report. If there are too many
     * sources to fit all the necessary RR packets into one compound RTCP packet
     * without exceeding the MTU of the network path, then only the subset that
     * will fit into one MTU SHOULD be included in each interval. The subsets
     * SHOULD be selected round-robin across multiple intervals so that all
     * sources are reported.
     *
     * @param statistics The statistics of the RTP session
     * @return The RTCP packet containing the RTCP Report (SS or RR).
     */
    public static RtcpPacket buildReport(RtpStatistics statistics) {
        // TODO Validate padding
        boolean padding = false;

        // Build the initial report packet
        RtcpReport report;
        if (statistics.hasSent()) {
            report = buildSenderReport(statistics, padding);
        } else {
            report = buildReceiverReport(statistics, padding);
        }

        // Build the SDES packet containing the CNAME
        RtcpSdes sdes = buildSdes(statistics, padding);

        // Build the compound packet
        return new DefaultRtcpPacket(report, sdes);
    }

    /**
     * Builds a packet containing an RTCP BYE message.
     *
     * @param statistics The statistics of the RTP session
     * @return The RTCP packet
     */
    public static RtcpPacket buildBye(RtpStatistics statistics) {
        // TODO Validate padding
        boolean padding = false;

        // Build the initial report packet
        RtcpReport report;
        if (statistics.hasSent()) {
            report = buildSenderReport(statistics, padding);
        } else {
            report = buildReceiverReport(statistics, padding);
        }

        // Build the SDES packet containing the CNAME
        RtcpSdes sdes = buildSdes(statistics, padding);

        // Build the BYE
        RtcpBye bye = new RtcpBye(padding);
        bye.addSsrc(statistics.getSsrc());

        // Build the compound packet
        return new DefaultRtcpPacket(report, sdes, bye);
    }

    public static RtcpPacket buildPacket(RtcpPacketType packetType, RtpStatistics statistics) {
        switch (packetType) {
            case RTCP_REPORT:
                return buildReport(statistics);

            case RTCP_BYE:
                return buildBye(statistics);

            default:
                throw new IllegalArgumentException("Unsupported RTCP packet type.");
        }
    }
}
