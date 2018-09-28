package com.sengled.media.server.rtsp.rtp;

import io.netty.buffer.ByteBufHolder;

/**
 * @author las
 * @date 18-9-28
 */
public interface RtpBody extends ByteBufHolder {

    RtpPayload payload();
}
