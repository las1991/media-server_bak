package com.sengled.media.server.rtsp.rtp.packetizer.h264;

import java.nio.ByteBuffer;
import com.sengled.media.server.MediaCodecExtra;

public class AVCDecoderConfigurationRecord implements MediaCodecExtra {
    private ByteBuffer sps;
    private ByteBuffer pps;
    private ByteBuffer profile;
    

    public byte[] getConfig() {
        ByteBuffer config = H264.makeExtra(this);
        
        byte[] data = new byte[config.remaining()];
        config.get(data);
        return data;
    }

    private ByteBuffer duplicate(ByteBuffer  src) {
        return null != src ? src.duplicate() : null;
    }

    public void setSPS_PPS(ByteBuffer sps, ByteBuffer pps) {
        setSPS_PPS(sps, pps, null);
    }
    
    public void setSPS_PPS(ByteBuffer sps, ByteBuffer pps, ByteBuffer profile) {
        if (null != sps && null != pps) {
            if (null == profile && sps.remaining() > 4) {
                profile = (ByteBuffer)sps.asReadOnlyBuffer().position(1).limit(4);
            }
            
            this.profile = profile;
            this.sps = sps;
            this.pps = pps;
        }
    }
    
    public boolean hasSPS_PPS() {
        return null != sps && sps.remaining() > 0 && null != pps && pps.remaining() > 0;
    }

    @Override
    public boolean isReady() {
        return hasSPS_PPS();
    }
    
    public ByteBuffer getSps() {
        return duplicate(sps);
    }
    public ByteBuffer getPps() {
        return duplicate(pps);
    }
    public ByteBuffer getProfile() {
        return duplicate(profile);
    }

}
