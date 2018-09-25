package com.sengled.cloud.devops.controller;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.alibaba.fastjson.JSON;
import com.sengled.cloud.devops.DevOpsMetricService;
import com.sengled.media.MediaSource;
import com.sengled.media.Version;
import com.sengled.media.bootstrap.route53.DomainTemplate;
import com.sengled.media.bootstrap.spring.HttpsProperties;
import com.sengled.media.clust.MediaServerClust;
import com.sengled.media.clust.server.MediaServerMetadata;
import com.sengled.media.server.rtsp.RtspServerContext;

@Controller
public class DevOpsController {
    @Value(value = "${PRIVATE_IPV4}")
    private String privateHost;

    @Value(value = "${PUBLIC_IPV4}")
    private String publicHost;
    
    @Autowired
    private HttpsProperties https;
    
    @Autowired
    private RtspServerContext context;

    @Autowired(required=false)
    private DevOpsMetricService devOps;
    
    @Autowired(required=false)
    private MediaServerClust clust;
    
    @Autowired
    private DomainTemplate domainTemplate;
    
    /**
     * uri: /monitor/getStreams
     * 兼容 spark 导入视频流
     *
     * @return
     */
    @GetMapping(path = {"/devops/streams", "/media/streams"})
    public ResponseEntity<?> getMediaStreams() {
        List<RtspSourceInfo> streams = new ArrayList<>();
        
        Collection<String> names = context.getMediaSourceNames();
        for (String name : names) {
            MediaSource resource = context.getMediaSource(name);
            if (null == resource) {
                continue;
            }
            
            streams.add(new RtspSourceInfo(resource, devOps.getServices(resource)));
        }
        
        return ResponseEntity.ok(JSON.toJSONString(streams));
    }
    
    @GetMapping("/devops/status")
    @ResponseBody
    public Map<String, Object> getDeviceStatus(@RequestParam("token") String token) {
        MediaSource resource = context.getMediaSource(token);
        
        HashMap<String, Object> result = new HashMap<>();
        if (null != resource) {
            result.put("status", "ok");
            result.put("data", new RtspSourceInfo(resource, devOps.getServices(resource)));
        } else {
            result.put("status", "DeviceNotFound");
        }
        
        return result;
    }
    
    @GetMapping("/devops/servers")
    @ResponseBody
    public Map<String, Object> getMediaServers() {
        
        final List<Object> datas;
        datas = 
        clust.getServerInstanceIds().stream().map(serverId -> {
            HashMap<String, Object> bean = new HashMap<>();

            MediaServerMetadata metadata = clust.getMetadata(serverId);
            bean.put("metadata", metadata);
            bean.put("runtime", clust.getRuntime(serverId));
            bean.put("score", clust.getScore(serverId));
            bean.put("host", "https://" + domainTemplate.getDomain(metadata.getPublicHost()) + ":" + https.getPort());
            return bean;
        }).collect(Collectors.toList());

        HashMap<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("selected", privateHost);
        result.put("data", datas);
       return result;
    }
    
    @GetMapping("/devops/version")
    @ResponseBody
    public String getVersion() {
        return Version.currentVersion();
    }
}
