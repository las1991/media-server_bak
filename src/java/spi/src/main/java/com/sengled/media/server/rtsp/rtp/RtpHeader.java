package com.sengled.media.server.rtsp.rtp;

import java.util.List;

/**
 * @author las
 * @date 18-9-21
 */
public interface RtpHeader {
    int version();

    boolean padding();

    boolean extension();

    int cc();

    boolean marker();

    int payloadType();

    int seqNumber();

    long time();

    long SSRC();

    List<Long> CSRC();

    int profile();

    int extensionLength();

    byte[] extensionHeader();

}
