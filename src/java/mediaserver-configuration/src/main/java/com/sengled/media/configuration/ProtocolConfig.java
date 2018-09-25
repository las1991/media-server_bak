package com.sengled.media.configuration;

public class ProtocolConfig {
    
    private int port;
    private boolean ssl;
    private boolean supportedHttp;
    private String methods;
    
    
    public void setSupportedHttp(boolean supportedHttp) {
        this.supportedHttp = supportedHttp;
    }
    
    
    public boolean isSupportedHttp() {
        return supportedHttp;
    }


    public void setPort(int port) {
        this.port = port;
    }
    
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }
    
    public int getPort() {
        return port;
    }
    
    public boolean isSsl() {
        return ssl;
    }
    
    public void setMethods(String methods) {
        this.methods = methods;
    }
    
    public String getMethods() {
        return methods;
    }

    public String getRtspUrl(String host, String token) {
        return (ssl ? "rtsps://" : "rtsp://") + host + ":" + port + "/" + token + ".sdp";
    }

    public String getHttpUrl(String host, String token) {
        return (ssl ? "https://" : "http://") + host + ":" + port + "/" + token + ".flv";
    }
}