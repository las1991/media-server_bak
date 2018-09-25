package com.sengled.cloud.devops;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.sengled.media.MediaSink;
import com.sengled.media.MediaSource;
import com.sengled.media.algorithm.AlgorithmSink;
import com.sengled.media.algorithm.AlgorithmSinkConfig;
import com.sengled.media.algorithm.config.Actions;
import com.sengled.media.event.EventBusListener;
import com.sengled.media.event.EventType;
import com.sengled.media.event.SessionEvent;
import com.sengled.media.plugin.capturer.CapturerMediaSink;
import com.sengled.media.server.rtsp.RtspServerContext;
import com.sengled.media.server.rtsp.RtspSource;

/**
 * 实现运维相关的需求
 * 
 * @author chenxh
 */
@Component
public class DevOpsMetricService implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevOpsMetricService.class);

    @Autowired
    private RtspServerContext serverContext;

    @Autowired
    private MetricRegistry registry;
    
    @Value("${PRIVATE_IPV4}")
    private String privateIp;
    
    @Value("${media.announcer.tcp.port}")
    private int rtspOverTcpPort;

    @Value("${media.announcer.tls.port}")
    private int rtspOverSSLPort;
    
    @Value("${DevOps.heartbeat.interval.minutes:15}")
    private int heartbeatIntervalInMinutes;
    
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // media 在线数量
        registry.register("media_online_total_num", new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return serverContext.getMediaSourceNames().size();
            }
        });

        // rtsp 播放器接入个数
        registry.register("player_online_total_num", new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                Collection<String> names = serverContext.getMediaSourceNames();
                return names.stream().mapToInt(new ToIntFunction<String>() {
                    @Override
                    public int applyAsInt(String value) {
                        MediaSource source = serverContext.getMediaSource(value);
                        
                        return null != source ? source.getMediaSinks(MediaSink.class).size() : 0;
                    }
                }).sum();
            }
        });
        
        // motion 在线数量
        registry.register("motion_online_total_num", gauge(serverContext, new Predicate<MediaSource>() {
            public boolean test(MediaSource source) {
                return isMotionOnline(source);
            };
        }));
        
        // 物体识别在线数量
        registry.register("object_online_total_num", gauge(serverContext, new Predicate<MediaSource>() {
            public boolean test(MediaSource source) {
                return isObjectOnline(source);
            };
        }));
        
        // 开通了算法服务的人
        registry.register("algorithm_online_total_num", gauge(serverContext, new Predicate<MediaSource>() {
            public boolean test(MediaSource source) {
                return isMotionOnline(source) || isObjectOnline(source);
            };
        }));
        
        // 录像在线数量
        registry.register("storage_online_total_num", gauge(serverContext, new Predicate<MediaSource>() {
            public boolean test(MediaSource source) {
                return isStorageOnline(source);
            }
        }));
        
        
        // 上下线日志
        serverContext.register(new EventBusListener<SessionEvent>() {
            
            @Override
            public void onEvent(SessionEvent sessionEvent) {
                String token = sessionEvent.getToken();
                if (isTalkbackSource(sessionEvent.getSource())) {
                    return; // talkback
                }

                MediaSource source = sessionEvent.getSource();
                boolean useSSL = useSSL(source);
               
                // 上下线事件
                DevOpsLogger.newWriter("media")
                      .withDeviceType("snap")
                      .withLogType("media_link_event")
                      .withDeviceIp(source.remoteAddress())
                      .withEventType(sessionEvent.getEventType())
                      .withSSL(useSSL)
                      .withToken(token)
                      .write();
            }
        });
        
        ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
        scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            
            @Override
            public void run() {
                RtspServerContext ctx = serverContext;
                Collection<String> names = ctx.getMediaSourceNames();
                for (String token : names) {
                    MediaSource rtspSource = ctx.getMediaSource(token);
                    if (null == rtspSource) {
                        continue;
                    }
                    
                    if (isTalkbackSource(rtspSource)) {
                        continue; // talkback
                    }
                    
                    // 心跳
                    DevOpsLogger.newWriter("media")
                        .withDeviceType("snap")
                        .withLogType("media_link_event")
                        .withDeviceIp(rtspSource.remoteAddress())
                        .withEventType(EventType.HeartBeat)
                        .withSSL(useSSL(rtspSource))
                        .withToken(token)
                        .withServices(getServices(rtspSource))
                        .write();
                }
                
            }
        }, heartbeatIntervalInMinutes, heartbeatIntervalInMinutes, TimeUnit.MINUTES);
        
        LOGGER.info("DevOps write heartbeat log in every {} minutes", heartbeatIntervalInMinutes);
    }
    
    private boolean isTalkbackSource(MediaSource source) {
        return source.getToken().contains("_");
    }
    
    private boolean useSSL(MediaSource mediaSource) {
        if (mediaSource instanceof RtspSource) {
            RtspSource source = (RtspSource)mediaSource;
            
            boolean useSSL = false;
            SocketAddress localAddress = source.localAddress();
            if (localAddress instanceof InetSocketAddress) {
                InetSocketAddress inet = (InetSocketAddress)localAddress;
                useSSL = rtspOverSSLPort == inet.getPort();
            }
            return useSSL;
        }
        
        return false;
    }
    
    private Gauge<Integer> gauge(RtspServerContext ctx, Predicate<MediaSource> filter) {
        return new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                int num = 0;
                
                Collection<String> names = ctx.getMediaSourceNames();
                for (String name : names) {
                    MediaSource rtspSource = ctx.getMediaSource(name);
                    if (null == rtspSource) {
                        continue;
                    }
                    
                    // 对结果过滤
                    if (filter.test(rtspSource)) {
                        num ++;
                    }
                }
                
                return num;
            }
        };
    }
    
    public final boolean isCaptureStarted(MediaSource source) {
        return null != source.getMediaSink(CapturerMediaSink.class);
    }
    
    public final boolean isMotionOnline(MediaSource source) {
        AlgorithmSink sink = source.getMediaSink(AlgorithmSink.class);
        if (null != sink) {
            final AlgorithmSinkConfig algorithmConfig = sink.getConfig();
            
            // 存在算法相关的配置
            if (!algorithmConfig.getStorageConfig().isEnable() || !algorithmConfig.getAlgorithmConfig().getActions().isEnable()) {
                return false;
            }
            
            // 存在 motion 识别的配置
            if (algorithmConfig.getAlgorithmConfig().getActions().getAlgorithms().contains(Actions.AlgorithmType.MOTION)) {
                return true;
            }
        }
        
        return false;
    }
    
    public final boolean isObjectOnline(MediaSource source) {
        AlgorithmSink sink = source.getMediaSink(AlgorithmSink.class);
        if (null != sink) {
            final AlgorithmSinkConfig algorithmConfig = sink.getConfig();
            
            // 存在算法相关的配置
            if (!algorithmConfig.getStorageConfig().isEnable() || !algorithmConfig.getAlgorithmConfig().getActions().isEnable()) {
                return false;
            }
            
            // 存在 motion 识别的配置
            if (algorithmConfig.getAlgorithmConfig().getActions().getAlgorithms().contains(Actions.AlgorithmType.PERSION)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isStorageOnline(MediaSource source) {
        // 针对存储 v3， 可以通过 url 判断
        List<MediaSink> sinks = source.getMediaSinks(MediaSink.class);
        for (MediaSink flvStreamingMediaSink : sinks) {
            if (flvStreamingMediaSink.isFromStorage()) {
                return true;
            }
        }
        
        return false;
    };
    
    public Services getServices(MediaSource source) {
        Services services = new Services();
        services.motion = isMotionOnline(source);
        services.screenshot = isCaptureStarted(source);
        services.object = isObjectOnline(source);
        services.storage = isStorageOnline(source);
        
        return services;
    }
    
    public static class Services {
        private boolean screenshot;
        private boolean motion;
        private boolean object;
        private boolean storage;
        
        public boolean isScreenshot() {
            return screenshot;
        }
        public boolean isMotion() {
            return motion;
        }
        public boolean isObject() {
            return object;
        }
        public boolean isStorage() {
            return storage;
        }
    }
}
