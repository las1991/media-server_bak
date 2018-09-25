package com.sengled.media.algorithm.webapp;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.sengled.media.MediaDispatcher;
import com.sengled.media.MediaSource;
import com.sengled.media.algorithm.AlgorithmSink;
import com.sengled.media.algorithm.AlgorithmSinkConfig;
import com.sengled.media.algorithm.MediaAlgorithmService;
import com.sengled.media.algorithm.QueryAlgorithmConfigRequest;
import com.sengled.media.algorithm.config.AlgorithmConfig;
import com.sengled.media.bootstrap.RestResultBuilder;
import com.sengled.media.device.GetDeviceRequest;
import com.sengled.media.device.MediaDeviceProfile;
import com.sengled.media.device.MediaDeviceService;
import com.sengled.media.plugin.config.storage.StorageConfig;
import com.sengled.media.plugin.kinesis.KinesisFrameStorer;
import com.sengled.media.server.rtsp.RtspServerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

@RestController
public class AlgorithmController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlgorithmController.class);
    
    @Autowired
    private RtspServerContext context;
    
    @Autowired
    private MediaAlgorithmService algorithmService;

    @Autowired
    private MediaDeviceService mediaDeviceService;
    
    @Autowired(required=false)
    private KinesisProducer kinesisProducer;
    
    @Value("${MEDIA_KINESIS_ALGORITHM}")
    private String streamName;
    
    @RequestMapping("/algorithm/startIfAbsent")
    public Map<String, Object> startIfAbsent(@RequestParam("token") String token, @RequestBody StorageConfig storage) throws InterruptedException, ExecutionException {
        MediaDeviceProfile profile = mediaDeviceService.getDeviceProfile(new GetDeviceRequest(token));
        AlgorithmConfig config =
        algorithmService.getAlgorithmConfig(new QueryAlgorithmConfigRequest().withToken(token));

        // 没有配置
        if (null == config || null == profile) {
            return Collections.singletonMap("status", "algorithm config not found");
        }
        
        // 获取视频源
        MediaSource source = context.getMediaSource(token);
        if (null == source) {
            return RestResultBuilder.newInstance().streamNotFound();
        }
        
        
        // 如果算法已经被关掉了， 则重启启动
        AlgorithmSinkConfig algorithmSinkConfig = new AlgorithmSinkConfig(profile, config, storage);
        return
        source.submit(new Function<MediaDispatcher, Map<String, Object>>() {
            @Override
            public Map<String, Object> apply(MediaDispatcher t) {
                AlgorithmSink sink = t.getMediaSink(AlgorithmSink.class);
                
                // 第一次启动
                if (null == sink) {
                    if (null == profile.getUserId()) {
                        LOGGER.error("[{}] {} 没有绑定用户.", token, profile.getUuid());
                        return Collections.singletonMap("status", "user of " + profile.getUuid() + " is NOT found");
                    } else if (!algorithmSinkConfig.isAlgorithmEnabled()){
                        LOGGER.info("[{}] {} 算法没有开启.", token, profile.getUuid());
                        return Collections.singletonMap("status", "algorithm is disabled");
                    } else {
                        LOGGER.info("[{}] {} 正在启动...", token, profile.getUuid());
                        new AlgorithmSink(source, new KinesisFrameStorer(kinesisProducer, streamName), algorithmSinkConfig).start();
                        return Collections.singletonMap("status", "starting");
                    }
                } else {
                    if (null == profile.getUserId()) {
                        LOGGER.error("[{}] {} 没有绑定用户.", token, profile.getUuid());
                        sink.close();
                    } else if (!algorithmSinkConfig.isAlgorithmEnabled()) {
                        LOGGER.info("[{}] {} 算法已经被关闭.", token, profile.getUuid());
                        sink.close();
                    } else if (!algorithmSinkConfig.isModified(sink.getConfig())) {
                        LOGGER.debug("[{}] {} 配置没有更新，维持现状.", token, profile.getUuid());
                    } else {
                        LOGGER.info("[{}] {} 配置更新了，准备重启...", token, profile.getUuid());
                        sink.close().addListener(new GenericFutureListener<Future<? super Void>>() {
                            public void operationComplete(Future<? super Void> future) throws Exception {
                                if (future.isSuccess()) {
                                    LOGGER.info("[{}] {} 配置更新了，正在重启...", token, profile.getUuid());
                                    new AlgorithmSink(source, new KinesisFrameStorer(kinesisProducer, streamName), algorithmSinkConfig).start();
                                } else {
                                    LOGGER.info("[{}] {} 配置更新了，但是重启失败!", token, profile.getUuid());
                                }
                            };
                        });
                    }
                }
                
                return null;
            }
        }).get();
    }
    

    @RequestMapping("/algorithm/stop")
    public Map<String, Object> stop(String token) throws InterruptedException, ExecutionException {
        boolean success = false;
        MediaSource source = context.getMediaSource(token);
        if (null != source) {
            success =
            source.submit(new Function<MediaDispatcher, Boolean>() {
                public Boolean apply(MediaDispatcher t) {
                    AlgorithmSink sink = t.getMediaSink(AlgorithmSink.class);
                    if (null != sink) {
                        try {
                            sink.close();
                            return true;
                        } catch (Exception e) {
                            LOGGER.error("[{}] Fail close algorithm sink", token, e);
                        }
                    }
                    
                    return false;
                };
            }).get();
        }
        
        return Collections.singletonMap("statue", success? "ok" : "fail");
    }
}
