package com.sengled.media.server.rtsp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.sengled.media.Key;
import com.sengled.media.MediaSource;
import com.sengled.media.apps.JavaScriptApplication;
import com.sengled.media.apps.JavaScriptApplicationManager;
import com.sengled.media.event.Event;
import com.sengled.media.event.EventBusListener;
import com.sengled.media.event.SessionEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;


/**
 * 一个 server 实例
 * 
 * @author 陈修恒
 * @date 2016年5月6日
 */

final public class RtspServerContext {

    private static final Logger logger = LoggerFactory.getLogger(RtspServerContext.class);

    private final String name;
    private final ConcurrentMap<Key, MediaSource> sources = new ConcurrentHashMap<>();

    private EventBus eventBus;
    

    /** 依赖的脚本 */
    private JavaScriptApplicationManager applications = new JavaScriptApplicationManager();

    public RtspServerContext(String name) {
        this.name = name;
        this.eventBus = new AsyncEventBus(name, Executors.newSingleThreadExecutor());
    }


    /**
     * 加载脚本用于监听 Media-Server 的事件
     * 
     * @param name
     * @param jsFile
     * @param runtimeVariables
     * @return
     * @throws IOException
     * @author chenxh
     */
    public JavaScriptApplication loadApplication(String name, File jsFile, Map<String, Object> runtimeVariables) throws IOException {
        JavaScriptApplication app = applications.loadApplication(name, jsFile, runtimeVariables);
        
        // 监听事件
        register(app);
        
        logger.info("load app [{}], {}", name, app);
        return app;
    }


    public void register(EventBusListener<? extends Event> listener) {
        getEventBus().register(listener);
    }
    
    public void unregister(EventBusListener<? extends Event> listener) {
        getEventBus().unregister(listener);
    }
    
    /**
     * 加载脚本用于监听 Media-Server 的事件
     * 
     * @param name
     * @param jsFile
     * @param runtimeVariables
     * @return
     * @throws IOException
     * @author chenxh
     */
    public JavaScriptApplication getApplication(String name) {
        return applications.getApplication(name);
    }
    

    public boolean removeMediaSource(final String name, final MediaSource source) {
        final boolean removed = sources.remove(Key.valueOf(name), source);
        if (removed) {
            eventBus.post(SessionEvent.newOfflineEvent(source));
            logger.info("[{}] {} device session(s) offline", name, sources.size());
        }

        return removed;
    }

    public void addMediaSource(final String name, final MediaSource source) {
        final MediaSource removed = sources.put(Key.valueOf(name), source);
        
        if (null != removed && removed != source) {
            eventBus.post(SessionEvent.newOfflineEvent(source));
            
            removed.close(); // close rtsp session
        }

        if (removed != source) {
            eventBus.post(SessionEvent.newOnlineEvent(source));
            logger.info("[{}] {} device session(s) online", name, sources.size());
        }
    }

    public void postEvent(Event event) {
        eventBus.post(event);
    }

    public Collection<String> getMediaSourceNames() {
        return Collections2.transform(sources.keySet(), new Function<Key, String>() {
            @Override
            public String apply(Key input) {
                return input.getToken();
            }
        });
    }

	public MediaSource getMediaSource(String name) {
	    return sources.get(Key.valueOf(name));
	}
    
    public int getMediaSourceSize() {
        return sources.size();
    }
    
    
    public void clear() {
        List<Future<Void>> closeFutures = new ArrayList<>();

        // 把脚本删除
        for (String appName : applications.getApplicationNames()) {
            JavaScriptApplication app = applications.removeApplication(appName);
            if (null != app) {
                unregister(app);
                logger.info("remove app [{}]", appName);
            }
        }

        // 关闭视频源
        for (Key key : new ArrayList<>(sources.keySet())) {
            MediaSource source = sources.remove(key);
            if(null != source) {
                final Future<Void> future = source.close();
                closeFutures.add(future);
                
                future.addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        logger.warn("[{}] cleared", source.getToken());
                    }
                });
            }
        }

        // 等待关闭完成
        closeFutures.forEach(new Consumer<Future<Void>>() {
            public void accept(Future<Void> t) {
                try {
                    t.get(3, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.error("Fail close MediaSource for {}", e.getMessage(), e);
                }
            };
        });


        this.eventBus = new AsyncEventBus(name, Executors.newSingleThreadExecutor());
    }


    public String getName() {
        return name;
    }
    
    
    public EventBus getEventBus() {
        return eventBus;
    }
    
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{").append(getClass().getSimpleName());
        buf.append(", name=").append(name);
        buf.append("}");
        return buf.toString();
    }
}
