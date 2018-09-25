package com.sengled.media.bootstrap.cache; 

import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCachePrefix;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class CacheConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfiguration.class);
    
    @Value("${spring.cache.defaultExpireTime:300}")
    private int defaultExpireTime = 300;

    @Value("${spring.cache.prefix}") 
    private String cachePrefix;
    
    @Value("${spring.cache.version}") 
    private String cacheVersion;
    
    private RedisCachePrefix prefix = new RedisCachePrefix() {
        @Override
        public byte[] prefix(String cacheName) {
            return (cachePrefix + ":" + cacheName + ":").getBytes();
        }
    };
    
    @Bean(name=RedisKeyGenerator.BEAN_NAME)
    public RedisKeyGenerator getDeviceIdCacheKeyGenerator() {
        return new RedisKeyGenerator(cacheVersion);
    }

    @Bean
    public CacheManager getCacheManager(SpringCacheRedisTemplate redisTemplate) {
        RedisCacheManager manager = new RedisCacheManager(redisTemplate);
        manager.setDefaultExpiration(defaultExpireTime);
        manager.setLoadRemoteCachesOnStartup(false);
        manager.setTransactionAware(true);
        manager.setUsePrefix(true); // 必须使用前缀
        manager.setCachePrefix(prefix);
        
        // 设备缓存用 15 分钟
        long deviceExpires = 15 * 60;
        manager.setExpires(Collections.singletonMap("device", deviceExpires));
        LOGGER.warn("device cache expires after {}s", deviceExpires);
        return manager;
    }

    @Bean()
    public SpringCacheRedisTemplate getRedisTemplate(RedisConnectionFactory factory) {
        return new SpringCacheRedisTemplate(factory);
    }
}
