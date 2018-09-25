package com.sengled.media.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "media.describer")
public class MediaDescriberProperties {
    private ProtocolConfig awsEcho = new ProtocolConfig();
    private ProtocolConfig app = new ProtocolConfig();

    public ProtocolConfig getAwsEcho() {
        return awsEcho;
    }

    public ProtocolConfig getApp() {
        return app;
    }
}
