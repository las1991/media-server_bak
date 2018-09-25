package com.sengled.media.resourcemanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestTemplate;
import com.sengled.media.Version;
import com.sengled.media.clust.MediaServerClust;
import com.sengled.media.clust.server.MediaResourceDao;
import com.sengled.media.clust.server.MediaServerDao;
import com.sengled.media.clust.server.MediaServerMetadata;
import com.sengled.media.configuration.MediaAnnouncerProperties;
import com.sengled.media.configuration.MediaDescriberProperties;

@Configuration()
@EnableCaching
@SpringBootApplication(scanBasePackageClasses=ResourceManagerConfiguration.class)
@ServletComponentScan(basePackageClasses=ResourceManagerConfiguration.class)
public class ResourceManagerConfiguration {
    static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerConfiguration.class);
    
    @Value("http://127.0.0.1:${server.port}/devops/streams")
    private String getStreamsUrl;
    
    @Value("http://127.0.0.1:${server.port}/devops/status?token=")
    private String getStatusUrl;
    
    @Value("${server.name}")
    private String serverName;
    
    private AtomicBoolean  running = new AtomicBoolean(true);
    
    @Bean
    public MediaServerMetadata getMediaServerMetadata(@Value("${PRIVATE_IPV4}") String privateHost,
                                            @Value("${PUBLIC_IPV4}") String publicHost,
                                            MediaAnnouncerProperties announcer,
                                            MediaDescriberProperties player) {
        MediaServerMetadata metadata = new MediaServerMetadata();
        
        metadata.setPrivateHost(privateHost);
        metadata.setPublicHost(publicHost);
        metadata.setVersion(Version.currentVersion());
        metadata.setInputPort(announcer.getTls().getPort());
        metadata.setOutputPort(player.getApp().getPort());
        metadata.setTcpPort(announcer.getTcp().getPort());
        
        return metadata;
    }
    
    @Bean
    public MediaResourceDao getMediaResourceDao(StringRedisTemplate redisTemplate) {
        return new MediaResourceDao() {};
    }
    
    @Bean
    public MediaServerDao getMediaServerDao(StringRedisTemplate redisTemplate) {
        return new MediaServerDao() {};
    }

    /**
     * @param redis
     * @param snapshot
     * @return
     */
    @Bean(destroyMethod="unregistLocal")
    public MediaServerClust getMediaServerClust(MediaServerMetadata snapshot, StringRedisTemplate redisTemplate) {
        MediaServerClust clust = new MediaServerClust(redisTemplate, snapshot, serverName);
        
        final Thread hook = new Thread(new Runnable() {
            @Override
            public void run() {
                if (running.compareAndSet(true, false)) {
                    LOGGER.info("system is shutting down");
                    clust.unregistLocal();
                }
            }
        });
        hook.setDaemon(true);
        hook.setName("MediaServerClustHook");
        Runtime.getRuntime().addShutdownHook(hook);
        return clust;
    }
    

    /**
     * 定时更新本服务的状态
     * 
     * @param clust
     */
    @Bean(name="registWorker")
    public ScheduledWorker regist(MediaServerClust clust, RestTemplate restTemplate) {
        return ScheduledWorker.simpleDelay(new Runnable() {
            @Override
            public void run() {
                // 最长 31 秒过期
                if (running.get()) {
                    List<Map<String, Object>> streams = restTemplate.getForObject(getStreamsUrl, ArrayList.class);
                    clust.registLocal(streams.size());
                } else {
                    LOGGER.error("system is NOT running");
                }
            }
        });
    }
    

    /**
     * 定时更新本机的 token 
     * 
     * @param clust
     * @param restTemplate
     */
    @Bean(name="updateMediaServerTokens")
    public ScheduledWorker updateMediaServerTokensWorker(MediaServerClust clust, MediaServerMetadata snapshot, RestTemplate restTemplate) {
        return ScheduledWorker.maxDelay(new Runnable() {
            @Override
            public void run() {
                // 获取视频流中的 token
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> streams = restTemplate.getForObject(getStreamsUrl, ArrayList.class);

                // 同步 resource server 中的 token
                streams.stream().map(new Function<Map<String, Object>, String>() {
                    @Override
                    public String apply(Map<String, Object> t) {
                        return (String) t.get("name");
                    }
                }).forEach(token -> {
                    clust.updateLocation(token);
                });
            }
        });
    }
}
