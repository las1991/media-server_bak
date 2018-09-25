package com.sengled.media.resourcemanager.webapp;

import com.sengled.media.clust.server.MediaServerMetadata;

/**
 * 返回公网的 IP 和端口
 * 
 * @author chenxh
 */
public class AccessStreamRecource implements ServiceResource {
    private  int resource_port = 2554;
    private  int resource_ssl_port = 1554;
    private  String resource_type = "talkback";
    private  String resource_addr = "101.68.222.221";
    
    
    public static ServiceResource media(MediaServerMetadata metadata) {
        AccessStreamRecource r = new AccessStreamRecource();
        
        r.resource_port = metadata.getTcpPort();
        r.resource_ssl_port = metadata.getInputPort();
        r.resource_type = "media";
        r.resource_addr = metadata.getPublicHost();
        
        return r;
    }
    
    
    public static ServiceResource talkback(MediaServerMetadata local, int tcpPort) {
        AccessStreamRecource r = new AccessStreamRecource();
        
        r.resource_port = tcpPort;
        r.resource_ssl_port = local.getInputPort();
        r.resource_type = "talkback";
        r.resource_addr = local.getPublicHost();
        
        
        return r;
    }
    
    
    /* (non-Javadoc)
     * @see com.sengled.media.resourcemanager.ServiceResource#getResource_addr()
     */
    @Override
    public String getResource_addr() {
        return resource_addr;
    }

    /* (non-Javadoc)
     * @see com.sengled.media.resourcemanager.ServiceResource#getResource_port()
     */
    @Override
    public int getResource_port() {
        return resource_port;
    }

    /* (non-Javadoc)
     * @see com.sengled.media.resourcemanager.ServiceResource#getResource_ssl_port()
     */
    @Override
    public int getResource_ssl_port() {
        return resource_ssl_port;
    }

    /* (non-Javadoc)
     * @see com.sengled.media.resourcemanager.ServiceResource#getResource_type()
     */
    @Override
    public String getResource_type() {
        return resource_type;
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        buf.append("{").append(getClass().getSimpleName());
        buf.append(", ").append(resource_type).append("=").append(resource_addr).append(":").append(resource_port);
        
        buf.append("}");
        return buf.toString();
    }

   
}
