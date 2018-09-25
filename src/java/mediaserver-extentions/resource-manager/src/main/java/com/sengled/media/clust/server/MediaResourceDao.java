package com.sengled.media.clust.server;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import com.sengled.media.bootstrap.cache.CacheModel;
import com.sengled.media.bootstrap.cache.RedisKeyGenerator;

public interface MediaResourceDao {
    @CacheEvict(cacheNames="device", keyGenerator=RedisKeyGenerator.BEAN_NAME)
    default public void removeDevice(@CacheModel("location") String token, String serverId) {
        
    }
    
    @CachePut(cacheNames="device", keyGenerator=RedisKeyGenerator.BEAN_NAME)
    default public String setDeviceLocation(@CacheModel("location") String token, String serverId){
        return serverId;
    }

    @Cacheable(cacheNames="device", keyGenerator=RedisKeyGenerator.BEAN_NAME)
    default public String getDeviceLocation(@CacheModel("location") String token) {
        return null;
    }
}
