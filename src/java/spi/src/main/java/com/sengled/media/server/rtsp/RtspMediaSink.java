package com.sengled.media.server.rtsp;

import com.sengled.media.MediaSource;
import com.sengled.media.server.rtsp.rtcp.RtcpPacket;
import com.sengled.media.server.rtsp.rtp.InterleavedRtpPacket;

/**
 * @author las
 * @date 18-9-21
 */
public interface RtspMediaSink extends MediaSource {

    void onRtp(InterleavedRtpPacket rtpPacket);

    void onRtcp(RtcpPacket rtcpPacket);
}
