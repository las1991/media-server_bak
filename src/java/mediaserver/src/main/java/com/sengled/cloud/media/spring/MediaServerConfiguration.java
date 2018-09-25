package com.sengled.cloud.media.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.sengled.media.configuration.MediaAnnouncerProperties;
import com.sengled.media.configuration.MediaDescriberProperties;
import com.sengled.media.server.rtsp.RtspServerContext;

/**
 * 负责 snap 的视频直播功能
 *
 * @author chenxh
 */
@Configuration
@ConfigurationProperties
public class MediaServerConfiguration {
    private MediaDescriberProperties mediaDescriberProperties = new MediaDescriberProperties();
    private MediaAnnouncerProperties mediaAnnouncerProperties = new MediaAnnouncerProperties();
    private RtspServerContext serverContext = new RtspServerContext("media");
    
    public MediaServerConfiguration() {
        
    }
    
    @Bean
    public MediaDescriberProperties getMediaDescriberProperties() {
        return mediaDescriberProperties;
    }

    @Bean
    public MediaAnnouncerProperties getMediaAnnouncerProperties() {
        return mediaAnnouncerProperties;
    }
    
    @Bean
    public RtspServerContext getRtspServerContext() {
        return serverContext;
    }

    @Bean
    public MediaServerContainer getMediaServerContainer() {
        return new MediaServerContainer(serverContext);
    }
   
    @Bean
    public MediaServerController getMediaServerController() {
        return new MediaServerController(serverContext);
    }
}
