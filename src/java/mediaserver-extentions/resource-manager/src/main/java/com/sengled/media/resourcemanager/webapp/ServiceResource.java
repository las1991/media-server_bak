package com.sengled.media.resourcemanager.webapp;

/**
 * 
 * # 这么古怪的命名，只是因为 python 时代的接口就这么定义的。
 * 
 * @author chenxh
 */
public interface ServiceResource {

    String getResource_addr();

    int getResource_port();

    int getResource_ssl_port();

    String getResource_type();

}