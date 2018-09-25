package com.sengled.cloud.devops.controller;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.time.DateFormatUtils;
import com.sengled.cloud.devops.DevOpsMetricService.Services;
import com.sengled.media.MediaSink;
import com.sengled.media.MediaSource;

public class RtspSourceInfo {
    private final Services services;
    
    private String name;
    private String created;

   
    private List<String> sinks = new ArrayList<>();

    public RtspSourceInfo(MediaSource source, Services services) {
        this.name = source.getToken();
        this.created = DateFormatUtils.format(source.getCreated(), "yyyy-MM-dd HH:mm:ss");
        this.services = services;
        
        List<MediaSink> sinks = source.getMediaSinks(MediaSink.class);
        
        // 有多少客户端
        for (MediaSink rtspSink : sinks) {
            this.sinks.add(String.valueOf(rtspSink));
        }
    }

    public String getName() {
        return name;
    }

    public String getCreated() {
        return created;
    }

    public List<String> getSinks() {
        return sinks;
    }

    public Services getServices() {
        return services;
    }
   
}
