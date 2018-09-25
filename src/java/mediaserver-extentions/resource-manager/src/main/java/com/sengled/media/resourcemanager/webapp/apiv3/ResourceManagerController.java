package com.sengled.media.resourcemanager.webapp.apiv3;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.sengled.media.bootstrap.RestResultBuilder;
import com.sengled.media.bootstrap.route53.DomainTemplate;
import com.sengled.media.clust.MediaServerClust;
import com.sengled.media.clust.RequestFrom;
import com.sengled.media.clust.server.MediaResourceDao;
import com.sengled.media.clust.server.MediaServerMetadata;
import com.sengled.media.server.rtsp.RtspServerContext;
import com.sengled.media.url.URLObject;

/**
 * 流媒体管理相关的内容
 * 
 * @author chenxh
 */
@Controller
public class ResourceManagerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerController.class);
    @Value("${PRIVATE_IPV4}")
    private String privateHost;
    
    @Value("${PUBLIC_IPV4}")
    private String publicHost;

    @Autowired
    private MediaServerClust clust;
    
    @Autowired
    private MediaResourceDao dao;
    
    @Autowired()
    private DomainTemplate domainTemplate;

    @Autowired()
    private RtspServerContext context;
    
    @RequestMapping("/addToken")
    @ResponseBody
    public String put(@RequestParam("token") String token) {
        dao.setDeviceLocation(token, "hahahha");
        
        return "a";
    }
    /**
     * 监听本地 media server 发出的事件
     * 
     * @param token
     * @param eventType
     */
    @RequestMapping("/media/events/{eventType}")
    @ResponseBody
    public String onEvent(
            @PathVariable(name="eventType") String eventType, 
            @RequestParam(name="token") String token, 
            @RequestParam(name="dn", required=false) String dn) {
        
        switch(eventType) {
        case "online":
            clust.updateLocation(token);
            break;
        case "offline":
            clust.removeLocation(token);
            break;
        }
        
        return "{\"status\":\"ok\"}";
    }

    
    /***
     * 申请对讲服务器
     * 
     * @param token
     * @return
     */
    @PostMapping("/media/stop")
    @ResponseBody
    public Map<String, Object> stopMediaServer(HttpServletRequest request) {
        String user = request.getHeader("user");
        if ("chenxh".equals(user) && "127.0.0.1".equals(request.getRemoteAddr())) {
            clust.unregistLocal();
            LOGGER.info("media server unregist");
            
            context.clear();
            LOGGER.info("media server source cleared");
            return Collections.singletonMap("status", "ok");
        }
        
        
        return Collections.singletonMap("status", "use local user please");
    }
    
    /***
     * 申请对讲服务器
     * 
     * @param token
     * @return
     */
    @GetMapping("/media/alloc")
    @ResponseBody
    public Map<String, Object> allocMediaServer(@RequestParam(name="token") String token) {
        MediaServerMetadata instance = clust.allocInstance(token);
        if (null != instance) {
            RestResultBuilder builder = RestResultBuilder.newInstance();
            
            // 因为 snap 灯不能解析域名，所以返回 ip
            builder.with("rtsps", "rtsps://" + instance.getPublicHost() + ":" + instance.getInputPort() + "/" + token + ".sdp");
            builder.with("rtsp", "rtsp://" + instance.getPublicHost() + ":" + 554 + "/" + token + ".sdp");
            
            return builder.ok();
        }

        // 没有合适的流媒体服务器实例
        return RestResultBuilder.newInstance().serverNotAvaliable();
    }

    /**
     * 一般 token 列表会很多， 所以强制使用 POST， 避免调用者误用 GET 造成 http url 太长
     * @param tokens
     * @return
     */
    //@PostMapping("/media/queryLiveUrls")
    @RequestMapping("/media/queryLiveUrls")
    @ResponseBody
    public Map<String, Object> queryLiveUrls(@Param("tokens") String tokens) {
        RestResultBuilder builder = RestResultBuilder.newInstance();
        
        Arrays.asList(StringUtils.split(tokens, ",")).stream().forEach(token -> {
            MediaServerMetadata metadata = clust.getLocation(token);
            if (null != metadata) {
                String rtsps = metadata.getUrl(token, RequestFrom.CAMERA, domainTemplate);
                String https = metadata.getUrl(token, RequestFrom.FLASH, domainTemplate);
                
                HashMap<String, String> urls = new HashMap<>();
                urls.put("rtsps", rtsps);
                urls.put("https", https);
                builder.with(token, urls);
            }
        });
        
        return builder.ok();
    }
    
    /***
     * 面向播放器开放的获取直播地址的 API 
     * 
     * amazon-storage, openapi 等服务会调用
     * @param token
     * @return
     */
    @GetMapping("/media/getLiveUrl")
    @ResponseBody
    public Map<String, Object> getLiveUrl(@RequestParam(name="token") String token, @RequestParam(name="from", required=false, defaultValue="unknown") String from) {
        RestResultBuilder builder = RestResultBuilder.newInstance();
        
        MediaServerMetadata metadata = clust.getLocation(token);
        if (null != metadata) {
            String url = metadata.getUrl(token, from, domainTemplate);
            
            if (StringUtils.equalsIgnoreCase(from, "aws-echo")) {
                // aws-echo 怪异的设计方案： rtsps 却要用 rtsp://
                builder.with("rtsps", url);
            } else {
                URLObject obj = URLObject.parse(url);
                builder.with(obj.getScheme(), url);
            }

            return builder.ok();
        } else {
            return RestResultBuilder.newInstance().streamNotFound();
        }
    } 
    
    /**
     * @param token
     * @param uid 用户随机数，用户客户端的唯一标示
     * @return
     */
    @GetMapping("/media/getTalkbackUrl")
    @ResponseBody
    public Map<String, Object> getTalkbackUrl(
                                @RequestParam(name="token") String token, 
                                @RequestParam(name="dn", required=false) String dn, 
                                @RequestParam(name="uid", required=false) String uid) {
        MediaServerMetadata shot = clust.local();
        
        RestResultBuilder builder = RestResultBuilder.newInstance();
        builder.with("rtsps", "rtsps://" + shot.getPublicHost() + ":" + shot.getInputPort() + "/" + token + "_" + uid + ".sdp?dn=" + dn);
        return builder.ok();
    }
    
    /***
     * 申请对讲服务器, 为了兼容老的播放器
     * 
     * @param token
     * @return
     */
    @GetMapping("/talkback/alloc")
    @ResponseBody
    @Deprecated
    public Map<String, Object> talkbackAlloc(@RequestParam(name="token") String token) {
        MediaServerMetadata shot = clust.local();
        
        RestResultBuilder builder = RestResultBuilder.newInstance();
        builder.with("rtsp", "rtsp://" + shot.getPublicHost() + ":" + shot.getInputPort() + "/" + token +  ".sdp");
        return builder.ok();
    }
    
    /**
     * 获取服务的内网 ip
     * 
     * @param token
     * @return
     */
    @GetMapping("/media/location")
    @ResponseBody
    public Map<String, Object> getLocation(
                                @RequestParam(name="token") String token) {
        MediaServerMetadata resource = clust.getLocation(token);
        if(null != resource ) {
            
            return RestResultBuilder.newInstance()
                        .with("location", resource.getPrivateHost())
                        .with("host", resource.getPublicHost())
                        .ok();
        }

        // 视频流不存在
        return RestResultBuilder.newInstance().streamNotFound();
    }
}
