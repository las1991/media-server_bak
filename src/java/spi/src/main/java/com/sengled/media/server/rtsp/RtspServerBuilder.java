package com.sengled.media.server.rtsp;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.MetricRegistry;
import com.sengled.media.UserAuthentication;

public class RtspServerBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(RtspServerBuilder.class);
    
    
    private UserAuthentication authentication;
    
    private MetricRegistry registry = new MetricRegistry();
    
    private List<Object> listeners = new ArrayList<>();
    
    public static RtspServerBuilder newInstance() {
        return new RtspServerBuilder();
    }
    
    
    public RtspServerBuilder withMetricRegistry(MetricRegistry registry) {
        this.registry = registry;
        
        return this;
    }
    
    
    public RtspServerBuilder withListener(Object listener) {
        this.listeners.add(listener);
        
        return this;
    }
    
    public RtspServer build(RtspServerContext ctx) {
        // 绑定事件监听器
        for (Object rtspServerEventListener : listeners) {
            ctx.getEventBus().register(rtspServerEventListener);
            LOGGER.info("{} used", rtspServerEventListener);
        }
        
        // 创建 rtsp server 实例
        LOGGER.info("new rtsp server '{}'", ctx.getName());
        return new RtspServer(ctx, registry);
    }
}
