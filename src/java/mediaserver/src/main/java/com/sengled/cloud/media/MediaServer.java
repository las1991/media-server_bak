package com.sengled.cloud.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import com.sengled.cloud.authication.AuthicationConfiguration;
import com.sengled.cloud.devops.DevOpsConfiguration;
import com.sengled.cloud.media.spring.MediaServerConfiguration;
import com.sengled.media.MediaBaseClustConfiguration;
import com.sengled.media.algorithm.AlgorithmConfiguration;
import com.sengled.media.bootstrap.cache.CacheConfiguration;
import com.sengled.media.bootstrap.kinesis.Kinesis;
import com.sengled.media.bootstrap.redis.Redis;
import com.sengled.media.bootstrap.route53.Route53;
import com.sengled.media.bootstrap.spring.ApplicationConfig;
import com.sengled.media.bootstrap.spring.DefaultBootApplication;
import com.sengled.media.bootstrap.spring.HttpsConfiguration;
import com.sengled.media.bootstrap.sqs.SQS;
import com.sengled.media.resourcemanager.ResourceManagerConfiguration;
import com.sengled.media.storage.StorageServer;


public class MediaServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServer.class);
    
    public static void main(String[] args) {
        final long startAt = System.currentTimeMillis();

        // spring 用到的配置文件
        if (null == System.getProperty(ApplicationConfig.SPRING_CONFIG_LOCATION)) {
            throw new IllegalArgumentException(
                    "please use -Dspring.config.location=classpath:/config/application.properties,classpath:/config/storage.properties,classpath:/config/storage.properties,file:/etc/sengled/sengled.properties");
        }

        // 全部启动
        SpringApplication.run(new Object[]{
                MediaBaseClustConfiguration.class,
                DefaultBootApplication.class,
                HttpsConfiguration.class,
                WebClientAutoConfiguration.class,
                // ZipkinAutoConfiguration.class,
                Redis.class,
                Kinesis.class,
                SQS.class,
                Route53.class,
                CacheConfiguration.class,
                ResourceManagerConfiguration.class,
                DevOpsConfiguration.class,
                AuthicationConfiguration.class,
                MediaServerConfiguration.class,
                StorageServer.class,
                AlgorithmConfiguration.class,
        }, args);
        
        
        LOGGER.info("MediaServer started, costed {} ms", (System.currentTimeMillis() - startAt));
    }
}
