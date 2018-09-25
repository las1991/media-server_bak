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

package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.mobicents.media.server.impl.rtp.rfc2833.DtmfOutput;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.memory.Frame;
import org.slf4j.LoggerFactory;

/**
 * Transmits RTP packets over a channel.
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpTransmitter {
	
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RtpTransmitter.class);
	
	// Channel properties
	private DatagramChannel channel;
	private final RtpClock rtpClock;
	private final RtpStatistics statistics;
	private boolean dtmfSupported;
	private final RTPOutput rtpOutput;
	private final DtmfOutput dtmfOutput;

	// Packet representations with internal buffers
	private final RtpPacket rtpPacket = new RtpPacket(RtpPacket.RTP_PACKET_MAX_SIZE, true);
	private final RtpPacket oobPacket = new RtpPacket(RtpPacket.RTP_PACKET_MAX_SIZE, true);
	
	// WebRTC
	private DtlsHandler dtlsHandler;
	private boolean secure;

	// Details of a transmitted packet
	private RTPFormats formats;
	private RTPFormat currentFormat;
	private long timestamp;
	private long dtmfTimestamp;
	private int sequenceNumber;

	public RtpTransmitter(final Scheduler scheduler, final RtpClock clock, final RtpStatistics statistics) {
		this.rtpClock = clock;
		this.statistics = statistics;
		this.dtmfSupported = false;
		this.rtpOutput = new RTPOutput(scheduler, this);
		this.dtmfOutput = new DtmfOutput(scheduler, this);
		this.sequenceNumber = 0;
		this.dtmfTimestamp = -1;
		this.timestamp = -1;
		this.formats = null;
		this.secure = false;
	}
	
	public void setFormatMap(final RTPFormats rtpFormats) {
		this.dtmfSupported = rtpFormats.contains(AVProfile.telephoneEventsID);
		this.formats = rtpFormats;
	}
	
	public RTPOutput getRtpOutput() {
		return rtpOutput;
	}
	
	public DtmfOutput getDtmfOutput() {
		return dtmfOutput;
	}
	
	public void enableSrtp(final DtlsHandler handler) {
		this.secure = true;
		this.dtlsHandler = handler;
	}

	public void disableSrtp() {
		this.secure = false;
		this.dtlsHandler = null;
	}
	
	public void activate() {
		this.rtpOutput.activate();
		this.dtmfOutput.activate();
	}
	
	public void deactivate() {
		this.rtpOutput.deactivate();
		this.dtmfOutput.deactivate();
		this.dtmfSupported = false;
	}
	
	public void setChannel(final DatagramChannel channel) {
		this.channel = channel;
	}
	
	private boolean isConnected() {
		return this.channel != null && this.channel.isConnected();
	}
	
	private void disconnect() throws IOException {
		if(this.channel != null) {
			this.channel.disconnect();
		}
	}
	
	public void reset() {
		deactivate();
		clear();
	}
	
	public void clear() {
		this.timestamp = -1;
		this.dtmfTimestamp = -1;
		// Reset format in case connection is reused.
		// Otherwise it would point to incorrect codec.
		this.currentFormat = null;
	}
	
	private void send(RtpPacket packet) throws IOException {
		// Do not send data while DTLS handshake is ongoing. WebRTC calls only.
		if(this.secure && !this.dtlsHandler.isHandshakeComplete()) {
			return;
		}
		
		// Secure RTP packet. WebRTC calls only. 
		// SRTP handler returns null if an error occurs
		ByteBuffer buffer = packet.getBuffer();
		if (this.secure) {
			byte[] rtpData = new byte[buffer.limit()];
			buffer.get(rtpData, 0, rtpData.length);
			byte[] srtpData = this.dtlsHandler.encodeRTP(rtpData, 0, rtpData.length);
			if(srtpData == null || srtpData.length == 0) {
				LOGGER.warn("Could not secure RTP packet! Packet dropped.");
				return;
			} else {
				buffer.clear();
				buffer.put(srtpData);
				buffer.flip();
			}
		}
		
		if(packet != null) {
			channel.send(buffer, channel.socket().getRemoteSocketAddress());
			// send RTP packet to the network and update statistics for RTCP
			statistics.onRtpSent(packet);
			
		}
	}
	
	public void sendDtmf(Frame frame) {
		if (!this.dtmfSupported) {
			frame.recycle();
			return;
		}
		
		// ignore frames with duplicate timestamp
		if (frame.getTimestamp() / 1000000L == dtmfTimestamp) {
			frame.recycle();
			return;
		}

		// convert to milliseconds first
		dtmfTimestamp = frame.getTimestamp() / 1000000L;
		// convert to rtp time units
		dtmfTimestamp = rtpClock.convertToRtpTime(dtmfTimestamp);
		oobPacket.wrap(false, AVProfile.telephoneEventsID, this.sequenceNumber++, dtmfTimestamp, this.statistics.getSsrc(), frame.getData(), frame.getOffset(), frame.getLength());

		frame.recycle();
		
		try {
			if(isConnected()) {
				send(oobPacket);
			}
		} catch (PortUnreachableException e) {
			try {
				// icmp unreachable received
				// disconnect and wait for new packet
				disconnect();
			} catch (IOException ex) {
				LOGGER.error(ex.getMessage(), ex);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	public void send(Frame frame) {
		// discard frame if format is unknown
		if (frame.getFormat() == null) {
			frame.recycle();
			return;
		}

		// determine current RTP format if it is unknown
		if (currentFormat == null || !currentFormat.getFormat().matches(frame.getFormat())) {
			currentFormat = formats.getRTPFormat(frame.getFormat());
			// discard packet if format is still unknown
			if (currentFormat == null) {
				frame.recycle();
				return;
			}
			// update clock rate
			rtpClock.setClockRate(currentFormat.getClockRate());
		}

		// ignore frames with duplicate timestamp
		if (frame.getTimestamp() / 1000000L == timestamp) {
			frame.recycle();
			return;
		}

		// convert to milliseconds first
		timestamp = frame.getTimestamp() / 1000000L;
		// convert to rtp time units
		timestamp = rtpClock.convertToRtpTime(timestamp);
		rtpPacket.wrap(false, currentFormat.getID(), this.sequenceNumber++, timestamp, this.statistics.getSsrc(), frame.getData(), frame.getOffset(), frame.getLength());

		frame.recycle();
		try {
			if (isConnected()) {
				send(rtpPacket);
			}
		} catch (PortUnreachableException e) {
			// icmp unreachable received
			// disconnect and wait for new packet
			try {
				disconnect();
			} catch (IOException ex) {
				LOGGER.error(ex.getMessage(), ex);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

}
