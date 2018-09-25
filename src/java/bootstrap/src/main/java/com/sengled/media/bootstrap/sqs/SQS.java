package com.sengled.media.bootstrap.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by las on 2017/3/22.
 */
@Configuration
@ConfigurationProperties
public class SQS {
    
    @Bean
    public SQSTemplate getSQSTemplate() {
        return new SQSTemplate();
    }

}
