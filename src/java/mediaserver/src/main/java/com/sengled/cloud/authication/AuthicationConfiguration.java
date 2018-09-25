package com.sengled.cloud.authication;

import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ServletComponentScan(basePackageClasses=AuthicationConfiguration.class)
@ComponentScan(basePackageClasses=AuthicationConfiguration.class)
public class AuthicationConfiguration {

}
