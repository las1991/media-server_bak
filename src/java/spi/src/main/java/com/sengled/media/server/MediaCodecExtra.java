package com.sengled.media.server;

/**
 * 编解码器的配置信息
 * 
 * @author chenxh
 */
public interface MediaCodecExtra {
    public boolean isReady();
    
    public byte[] getConfig();
}
