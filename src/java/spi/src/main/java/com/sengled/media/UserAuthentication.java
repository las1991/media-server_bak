package com.sengled.media;

import io.netty.handler.codec.http.HttpRequest;

/**
 * 设备的身份验证信息
 * 
 * @author 陈修恒
 * @date 2016年5月30日
 */
public interface UserAuthentication {
    
    /**
     * @param uri 设备的 url
     * @param headers 验证信息
     * @return true 表示这个验证是通过的，可以向服务器推送流
     */
    public boolean allowAnounce(HttpRequest request);

    
    /**
     * @param uri 设备的 url
     * @param headers 验证信息
     * @return true 表示这个验证是通过的，可以从服务器获取流
     */
    public boolean allowDescribe(HttpRequest request);
}
