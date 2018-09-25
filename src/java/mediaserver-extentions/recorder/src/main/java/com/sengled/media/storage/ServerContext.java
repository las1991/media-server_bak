package com.sengled.media.storage;

import java.util.Collection;

/**
 * 注册到 Redis 中的服务实例信息
 * 
 * @author chenxh
 */
public interface ServerContext {

    int numTask();

    Collection<String> getTaskNames();
}
