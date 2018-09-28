package com.sengled.media.server.rtsp.rtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

/**
 * @author las
 * @date 18-9-28
 */
public class RtpPayload extends DefaultByteBufHolder {
    
    public RtpPayload(ByteBuf data) {
        super(data);
    }
}
