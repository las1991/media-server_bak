package com.sengled.media.clust.server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sengled.media.Version;
import com.sengled.media.bootstrap.route53.DomainTemplate;
import com.sengled.media.clust.RequestFrom;

public class MediaServerMetadata {
    private String publicHost;
    private String privateHost;

    private String version = Version.currentVersion();

    /** 设备接入的端口 **/
    private int inputPort = 1554;
    
    /** 视频播放的端口 */
    private int outputPort = 1443;
    
    /** 内部端口 */
    private int tcpPort = 554;

    public MediaServerMetadata(){}
    
    @JsonIgnore
    public String getId() {
        return privateHost;
    }


    public String getPublicHost() {
        return publicHost;
    }

    public void setPublicHost(String publicHost) {
        this.publicHost = publicHost;
    }

    public String getPrivateHost() {
        return privateHost;
    }

    public void setPrivateHost(String privateHost) {
        this.privateHost = privateHost;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getInputPort() {
        return inputPort;
    }

    public void setInputPort(int sslPort) {
        this.inputPort = sslPort;
    }

    public int getOutputPort() {
        return outputPort;
    }

    public void setOutputPort(int playPort) {
        this.outputPort = playPort;
    }
    
    public void setTcpPort(int adminPort) {
        this.tcpPort = adminPort;
    }
    
    public int getTcpPort() {
        return tcpPort;
    }

    public String getUrl(String token, String from, DomainTemplate domainTemplate) {
        String protocol;
        String domain;
        String suffix;
        int port;
        switch(from) {
        case RequestFrom.MEDIA_PROXY:
            protocol = "rtsp";
            domain = privateHost;
            suffix = "sdp";
            port = getTcpPort();
            break;
        case RequestFrom.AWS_ECHO:
            protocol = "rtsp";
            domain =  domainTemplate.getDomain(publicHost); // domains.get(DomainNames.AWS_ECHO);
            suffix = "sdp";
            port = 443;
            break;
        case RequestFrom.FLASH:
            protocol = "https";
            domain = domainTemplate.getDomain(publicHost);
            suffix = "flv";
            port = getOutputPort();
            break;
        case RequestFrom.AMAZON_STORAGE:
        case RequestFrom.CAMERA:
        default:
            protocol = "rtsps";
            domain = publicHost;
            suffix = "sdp";
            port = getOutputPort();
            break;
        }
        
        if (null != domain) {
            return protocol + "://" + domain + ":" + port + "/" + token + "." + suffix;
        } else {
            return null;
        }
    }
}
