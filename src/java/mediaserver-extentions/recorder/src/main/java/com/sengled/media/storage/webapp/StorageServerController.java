package com.sengled.media.storage.webapp;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.sengled.media.MediaSource;
import com.sengled.media.device.GetDeviceRequest;
import com.sengled.media.device.MediaDeviceProfile;
import com.sengled.media.device.MediaDeviceService;
import com.sengled.media.file.segment.MediaSinkSpliterator;
import com.sengled.media.server.rtsp.RtspServerContext;
import com.sengled.media.storage.services.AsyncStorageHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * 用来申请存储服务
 *
 * 备注：只适用于 token 在本机的 RTSP 流
 *
 * @author chenxh
 */
@Controller
public class StorageServerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageServerController.class);

    @Value("${media.announcer.tcp.port}")
    private int announcerTcpPort;
    
    @Value("${mergeFile.root.directory}")
    private String mergeRootDirectory;
    
    @Value("${storage.flvsegment.durationInSeconds}")
    private int flvSegmentDurationInSeconds;
    
    @Autowired
    private RtspServerContext rtspServer;
    
    @Autowired
    private AsyncStorageHandler  asyncStorageHandler;    

    @Autowired
    private MediaDeviceService mediaDeviceService;
    
    
    @PostMapping("/storage/stopAll")
    @ResponseBody
    public Map<String, Object> stopAll(HttpServletRequest request) {
    	String remoteHost = request.getRemoteAddr();
    	if (!"127.0.0.1".equals(remoteHost)) {
    		return Collections.singletonMap("status", "forbidden");
    	}
    	
    	// 停掉所有的录像
    	Collection<String> names = rtspServer.getMediaSourceNames();
    	names.stream().forEach(token -> {
    		try {
    			stop(token);
    			LOGGER.info("stop recoder of {}", token);
    		} catch (Exception e) {
				LOGGER.warn("Fail stop record {}", token, e);
			}
    	});
        asyncStorageHandler.mergeSync(); //手动触发合并（不上传，因为会导致关不掉服务）
    	return Collections.singletonMap("status", "ok");
    }
    
    @GetMapping("/storage/stop")
    @ResponseBody
    public Map<String, Object> stop(@RequestParam(name = "token") String token) {
        MediaSource source = rtspServer.getMediaSource(token);
    	if (null == source) {
    		return Collections.singletonMap("statue", "StreamNotFound");
    	}
        MediaSinkSpliterator spliter = source.getMediaSink(StorageMediaSinkSpliterator.class);
        if (null != spliter) {
            spliter.close();
        }
    	return Collections.singletonMap("statue", "ok");
    }

    @GetMapping("/storage/startIfAbsent")
    @ResponseBody
    public Map<String, Object> startIfAbsent(@RequestParam(name = "token") String token,
        @RequestParam(name = "storageTime") int storageTime, @RequestParam(name = "timeZone") String timeZoneCity) {

        MediaSource source = rtspServer.getMediaSource(token);
    	if (null == source) {
    		return Collections.singletonMap("statue", "StreamNotFound");
    	}
    	
    	String flvDir = mergeRootDirectory + "/flvfile/" +  storageTime;
    	File flvTmpDir = new File(flvDir);
    	if(  ! flvTmpDir.exists() ) {
    	    if( ! flvTmpDir.mkdirs() ){
    	        LOGGER.error("dir create error. dir:{}", flvDir);
    	        return Collections.singletonMap("statue", "error");
    	    }
    	}
    	
    	// 如果设备没有绑定用户，则直接把录像任务关掉
    	MediaDeviceProfile profile = mediaDeviceService.getDeviceProfile(new GetDeviceRequest(token));
    	if (null == profile || null == profile.getUserId()) {
    	    StorageMediaSinkSpliterator recorder =  source.getMediaSink(StorageMediaSinkSpliterator.class);
    	    if (null != recorder) {
    	        recorder.close();
    	    }
    	    
    	    LOGGER.warn("[{}] illegal profile {}", token, profile);
    	    return Collections.singletonMap("statue", "failed");
    	}
    	
        StorageMediaSinkSpliterator spliter = source.getMediaSink(StorageMediaSinkSpliterator.class);
        if(null != spliter && spliter.isModified(storageTime, profile)) {
            // 存储计划变了，重启启动
            spliter.close().addListener(new GenericFutureListener<Future<Void>>() {
               public void operationComplete(Future<Void> future) throws Exception {
                   new StorageMediaSinkSpliterator(source, flvTmpDir,timeZoneCity,storageTime, flvSegmentDurationInSeconds,profile.getUserId()).start();
               }; 
            });
        } else if (null == spliter) {
            // 第一次启动录像
            new StorageMediaSinkSpliterator(source, flvTmpDir,timeZoneCity,storageTime, flvSegmentDurationInSeconds,profile.getUserId()).start();
        }
        
    	return Collections.singletonMap("statue", "ok");
    }
}
