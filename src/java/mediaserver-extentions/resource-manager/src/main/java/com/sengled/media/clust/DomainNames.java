package com.sengled.media.clust;

public interface DomainNames {
    
    /** 内网专用, 格式为 host:port */
    static final String LOCAL = "local";

    /** ssl, 格式为：  rtsp://  */
    static final String AWS_ECHO = "aws-echo";

    /** ssl, 格式为 rtsps:// */
    static final String SSL = "rtsps";
    

    /** ssl, 格式为 https:// */
    static final String HTTPS = "https";
}