package com.sengled.media.resourcemanager.webapp.apiv1;

import org.apache.commons.lang.StringUtils;

public class ServerInfo {
    private final String id;
    
    private final String host;
    private final int port;
    
    private String publicHost;
    private int connections;


    public ServerInfo(String id, String host, int port, String publicHost, Double curConnection) {
        super();
        this.id = id;
        this.host = host;
        this.port = port;
        this.publicHost = publicHost;
        this.connections = null != curConnection ? curConnection.intValue() : 0;
    }
    
    public String getPublicHost() {
        return publicHost;
    }

    
    public String getId() {
        return id;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    
    public int getConnections() {
        return connections;
    }
    
    public String getHttpUrl(String uri) {
        if (!StringUtils.startsWith(uri, "/")) {
            throw new IllegalArgumentException("uri must start with '/', but real is '" + uri + "'");
        }
        
        return "http://" + host + ":" + port + uri;
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append("{");
        buf.append("id=").append(id);
        buf.append(", ").append(publicHost).append(":").append(port);
        buf.append("}");
        return buf.toString();
    }
}
