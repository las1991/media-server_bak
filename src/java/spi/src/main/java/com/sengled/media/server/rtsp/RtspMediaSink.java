package com.sengled.media.server.rtsp;

import com.sengled.media.Observable;
import com.sengled.media.server.rtsp.rtcp.RtcpPacket;
import com.sengled.media.server.rtsp.rtp.RtpPacket;

/**
 * @author las
 * @date 18-9-21
 */
public interface RtspMediaSink extends Observable {

    void onRtp(RtpPacket rtpPacket);

    void onRtcp(RtcpPacket rtcpPacket);
}
