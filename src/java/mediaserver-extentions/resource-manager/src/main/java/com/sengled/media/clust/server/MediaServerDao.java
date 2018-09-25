package com.sengled.media.clust.server;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import com.sengled.media.bootstrap.cache.CacheModel;
import com.sengled.media.bootstrap.cache.RedisKeyGenerator;

public interface MediaServerDao {

    @CacheEvict(cacheNames="media-server", keyGenerator=RedisKeyGenerator.BEAN_NAME)
    default public void removeServer(@CacheModel("*") String serverId){}
    
    @Cacheable(cacheNames="media-server", keyGenerator=RedisKeyGenerator.BEAN_NAME)
    default public MediaServerMetadata getMetadata(@CacheModel("metadata") String serverId) {
        return null;
    }
    
    @CachePut(cacheNames="media-server", keyGenerator=RedisKeyGenerator.BEAN_NAME)
    default public MediaServerMetadata setMetadata(@CacheModel("metadata") String serverId, MediaServerMetadata metadata){
        return metadata;
    }
    
    @CacheEvict(cacheNames="media-server", keyGenerator=RedisKeyGenerator.BEAN_NAME)
    default public void deleteMetadata(@CacheModel("metadata") String serverId){
    }
    
    @CachePut(cacheNames="media-server", keyGenerator=RedisKeyGenerator.BEAN_NAME)
    default public MediaServerRuntime setRuntime(@CacheModel("runtime") String serverId, MediaServerRuntime runtime) {
        return runtime;
    }

    @CacheEvict(cacheNames="media-server", keyGenerator=RedisKeyGenerator.BEAN_NAME)
    default public MediaServerRuntime deleteRunTime(@CacheModel("runtime") String serverId) {
        return null;
    }
    
    @Cacheable(cacheNames="media-server", keyGenerator=RedisKeyGenerator.BEAN_NAME)
    default public MediaServerRuntime getRunTime(@CacheModel("runtime") String serverId) {
        return null;
    }
}
