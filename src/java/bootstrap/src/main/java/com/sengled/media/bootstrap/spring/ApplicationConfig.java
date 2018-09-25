package com.sengled.media.bootstrap.spring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.AutoCloseInputStream;

/**
 * 系统使用的参数
 *
 * @author chenxh
 */
public class ApplicationConfig {
    private ApplicationConfig() {
    }

    /**
     * 系统运行时参数，保存 spring 依赖的配置文件
     */
    public static final String SPRING_CONFIG_LOCATION = "spring.config.location";

    public static final String KINESIS_ALGORITHM_LOCATION = "kinesis.config.algorithm.location";
    
    public static InputStream read(String url) throws IOException {
        InputStream in;
        if (url.startsWith("classpath:")) {
            in = ApplicationConfig.class.getResourceAsStream(url.substring("classpath:".length()));
        } else if (url.startsWith("file:")) {
            in = new FileInputStream(url.substring("file:".length()));
        } else {
            in = new FileInputStream(url);
        }

        return new AutoCloseInputStream(in);
    }
}
