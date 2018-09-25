package com.sengled.cloud.devops;

import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.sengled.cloud.devops.mdflags.MdFlagsWebSocketConfig;

@Configuration
@ServletComponentScan(basePackageClasses=DevOpsConfiguration.class)
@ComponentScan(basePackageClasses=DevOpsConfiguration.class)
public class DevOpsConfiguration {

    public MdFlagsWebSocketConfig MdFlagsWebSocketConfig() {
        return new MdFlagsWebSocketConfig();
    }
}
