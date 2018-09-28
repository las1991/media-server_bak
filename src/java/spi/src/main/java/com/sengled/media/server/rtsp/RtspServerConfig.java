package com.sengled.media.server.rtsp;

import org.apache.commons.lang.StringUtils;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.rtsp.RtspMethods;

public class RtspServerConfig {
    private static final String[] DEFAULTS_METHODS;
    static {
        DEFAULTS_METHODS = new String[]{
                RtspMethods.OPTIONS.name(),
                RtspMethods.DESCRIBE.name(),
                RtspMethods.SETUP.name(),
                RtspMethods.PLAY.name(),
                RtspMethods.TEARDOWN.name()
        };
    }
    
    private String host = "0.0.0.0";
    private int port = 554;
    private boolean useSSL = false;
    private boolean useHTTPProtocol = false;
    private boolean useRTSPProtocol = false;
    
    private String[] methods = DEFAULTS_METHODS;

    public static RtspServerConfig newInstance(boolean useSSL) {
        RtspServerConfig config = new RtspServerConfig();
        config.useSSL = useSSL;
        
        return config;
    }
    
    private RtspServerConfig() {
        
    }
    
    public RtspServerConfig withHttpProtocol(boolean useHTTPProtocol) {
        this.useHTTPProtocol = useHTTPProtocol;
        return this;
    }
    
    public RtspServerConfig withMethods(String[] methods) {
        this.methods = methods;
        return this;
    }
    
    public RtspServerConfig withRtspProtocol(boolean useRTSPProtocol) {
        this.useRTSPProtocol = useRTSPProtocol;
        return this;
    }
    
    public RtspServerConfig withPort(int port) {
        this.port = port;
        
        return this;
    }
    
    
    public RtspServerConfig withHost(String host) {
        this.host = host;
        
        return this;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public boolean isUseHTTPProtocol() {
        return useHTTPProtocol;
    }

    public boolean isUseRTSPProtocol() {
        return useRTSPProtocol;
    }
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }

    public HttpMethod[] getMethods() {
        HttpMethod[] ms = new HttpMethod[this.methods.length];
        for (int i = 0; i < ms.length; i++) {
            ms[i] = RtspMethods.valueOf(StringUtils.trim(methods[i]));
        }
        return ms;
    }
}
