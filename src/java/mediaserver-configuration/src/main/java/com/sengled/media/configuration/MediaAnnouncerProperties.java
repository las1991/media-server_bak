package com.sengled.media.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="media.announcer")
public class MediaAnnouncerProperties {
    /**
     * 
     */
    private ProtocolConfig tls = new ProtocolConfig();
    
    private ProtocolConfig tcp = new ProtocolConfig();

    @Deprecated
    private ProtocolConfig talkback = new ProtocolConfig();
    
    
    public boolean isSSL(int port) {
        return isSSL(tls, port) || isSSL(talkback, port);
    }
    
    private static boolean isSSL(ProtocolConfig config, int port) {
        boolean equals = config.getPort() == port;
        
        return equals && config.isSsl();
    }
    
    
    public ProtocolConfig getTalkback() {
        return talkback;
    }
    
    
    public ProtocolConfig getTcp() {
        return tcp;
    }
    
    public ProtocolConfig getTls() {
        return tls;
    }
}
