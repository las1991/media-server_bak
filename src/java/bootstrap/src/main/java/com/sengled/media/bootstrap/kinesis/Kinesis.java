package com.sengled.media.bootstrap.kinesis;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.sengled.media.bootstrap.AmazonAwsConfig;
import com.sengled.media.bootstrap.spring.ApplicationConfig;

/**
 * Created by las on 2017/2/23.
 */
@Configuration
@ConfigurationProperties
public class Kinesis {

    static final Logger LOGGER = LoggerFactory.getLogger(Kinesis.class);



    @Autowired
    private AmazonAwsConfig awsConfig;


    @Bean(destroyMethod="destroy")
    public KinesisProducer algorithmKinesisProducer() {
        String file = System.getProperty(ApplicationConfig.KINESIS_ALGORITHM_LOCATION);
        KinesisProducerConfiguration config;
        if (StringUtils.isEmpty(file)) {
            config = new KinesisProducerConfiguration()
                    .setCredentialsProvider(awsConfig.getCredentialsProvider())
                    .setRegion(awsConfig.getRegion())
                    .setLogLevel("error")
                    .setMaxConnections(48)
                    .setRequestTimeout(6000)
                    .setRecordMaxBufferedTime(1000);
        } else {
            Properties props = new Properties();
            try (InputStream is = ApplicationConfig.read(file)) {
                props.load(is);
                LOGGER.info("{} loaded", file);
            } catch (Exception e) {
                throw new RuntimeException("Error loading config from properties file", e);
            }
            config = KinesisProducerConfiguration
                    .fromProperties(props)
                    .setCredentialsProvider(awsConfig.getCredentialsProvider())
                    .setRegion(awsConfig.getRegion());
        }
        
        KinesisProducer producer = new KinesisProducer(config);
        return producer;
    }
}
