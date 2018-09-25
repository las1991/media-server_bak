package com.sengled.media.bootstrap.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Pool;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration()
@ConfigurationProperties
public class Redis  {
    private static final Logger LOGGER  = LoggerFactory.getLogger(Redis.class);

	@Value("${MEDIA_REDIS_ADDR}")
	private String host = "localhost";
	
	@Value("${MEDIA_REDIS_PWD}")
	private String password;
	
	@Value("${MEDIA_REDIS_PORT}")
	private int port = 6379;
	
	@Value("${MEDIA_REDIS_DB:0}")
	private int database = 0;
	
	@Value("${redis.timeout}")
	private int timeout = 1000;
	
	@Value("${redis.maxIdle}")
	private int maxIdle;
	
	@Value("${redis.minIdle}")
	private int minIdle;

	@Value("${redis.maxActive}")
	private int maxActive;
	
	@Value("${redis.maxWait}")
	private int maxWait;
	
	//@Value("${redis.testOnBorrow}")
	//private boolean testOnBorrow;

	@Primary
	@Bean(name = "org.springframework.autoconfigure.redis.RedisProperties")
	public RedisProperties getRedisProperties() {
		RedisProperties props = new RedisProperties();
		
		props.setHost(host);
		props.setPassword(password);
		props.setPort(port);
		props.setTimeout(timeout);
		props.setDatabase(database);
		LOGGER.info("redis://{}:{}/{}?timeout={} used", host, port, database, timeout);
		
		
		Pool pool = new Pool();
		pool.setMaxActive(maxActive);
		pool.setMaxIdle(maxIdle);
		pool.setMaxWait(maxWait);
		pool.setMinIdle(minIdle);
		props.setPool(pool);
		
		return props;
	}
}
