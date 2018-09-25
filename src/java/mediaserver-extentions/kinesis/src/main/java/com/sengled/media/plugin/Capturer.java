package com.sengled.media.plugin;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.MediaSource;
import com.sengled.media.plugin.capturer.CapturerMediaSink;
import com.sengled.media.server.rtsp.RtspServerContext;
import com.sengled.media.server.rtsp.RtspSource;

/**
 * Created by las on 2017/3/6.
 */
public class Capturer {
    private static final ScheduledExecutorService randomThread = Executors.newScheduledThreadPool(1);
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Capturer.class);
    
    public static boolean start(RtspServerContext ctx, String token, FrameStorer storer, int interval) {
        
        // 延时开启截图，避免同时上线导致 kinesis 压力太大
        final long startAt = System.currentTimeMillis();
        randomThread.schedule(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                MediaSource resource = ctx.getMediaSource(token);
                if (null == resource) {
                    return null;
                }
                
                boolean noCapturerWork = null == resource.getMediaSink(CapturerMediaSink.class);
                if (noCapturerWork) {
                    new CapturerMediaSink(resource, storer, interval).start();
                    LOGGER.info("[{}] start capturer at {}ms delay", token, System.currentTimeMillis() - startAt);
                } else {
                    LOGGER.debug("[{}] capturer work is running", token);
                }
                
                return null;
            }
        }, 10, TimeUnit.SECONDS);
       
        
        return true;
    }

    public static boolean stop(RtspServerContext ctx, String token) {
        MediaSource src = ctx.getMediaSource(token);
        if (null == src) {
            return true;
        }
        
        CapturerMediaSink group = src.getMediaSink(CapturerMediaSink.class);
        group.close();
        
        return true;
    }

}
