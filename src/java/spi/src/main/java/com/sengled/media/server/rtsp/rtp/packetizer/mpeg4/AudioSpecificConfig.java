package com.sengled.media.server.rtsp.rtp.packetizer.mpeg4;

import java.nio.ByteBuffer;
import com.sengled.media.server.MediaCodecExtra;

public class AudioSpecificConfig implements MediaCodecExtra {
    ByteBuffer config;
	
	
	public AudioSpecificConfig() {
		super();
	}
	
	public AudioSpecificConfig(ByteBuffer config) {
        this.config = config;
    }

	@Override
	public boolean isReady() {
	    return null != config && config.hasRemaining();
	}

    public byte[] getConfig() {
        if (null != config) {
            byte[] data = new byte[config.remaining()];
            config.duplicate().get(data);
            return data;
        }
        
        return null;
    }
    
}
