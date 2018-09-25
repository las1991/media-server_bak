package com.sengled.media.storage;

import java.sql.Timestamp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.sengled.media.bootstrap.spring.DefaultBootApplication;

@SpringBootApplication(scanBasePackageClasses = StorageServer.class)
public class StorageServer {
    public static final String SPRING_CONFIG_LOCATION = "spring.config.location";

    public static void main(String[] args) {
        final long startAt = System.currentTimeMillis();
        if (null == System.getProperty(SPRING_CONFIG_LOCATION)) {
            throw new IllegalArgumentException(
                    "Not Found Env property '-Dspring.config.location', please use -Dspring.config.location=classpath:/config/application.properties,file:/etc/sengled/sengled.properties");
        }
        SpringApplication.run(new Object[] {
                DefaultBootApplication.class, 
                StorageServer.class
                }, args);
        System.out.println("media-storage-v3  started at " + new Timestamp(System.currentTimeMillis()) + ", cost "
                + (System.currentTimeMillis() - startAt) + "ms");
    }
}
