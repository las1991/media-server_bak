package com.sengled.media.resourcemanager.webapp.apiv1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sengled.media.clust.MediaServerClust;
import com.sengled.media.clust.server.MediaServerMetadata;
import com.sengled.media.configuration.MediaAnnouncerProperties;
import com.sengled.media.resourcemanager.webapp.AccessStreamRecource;
import com.sengled.media.resourcemanager.webapp.ResourceManagerResponse;

/**
 * 查找可用的资源
 * 
 * @author chenxh
 */
@Controller
public class AvaiableResourceController {
    
    @Autowired
    private MediaServerClust clust;
    
    @Autowired
    private MediaAnnouncerProperties announcer;
    
    /**
     * 请求算法、截图、存储服务会调用本接口
     * @param resource_type
     * @param token
     * @return
     */
    @RequestMapping(path = { "/{resource_type}/available_resource/", "/{resource_type}/available_resource" })
    @ResponseBody
    public ResourceManagerResponse getAvailableResource(@PathVariable("resource_type") String resource_type,  @RequestParam("token") String token) {
        
        switch(resource_type) {
        case "talkback":
            MediaServerMetadata local = clust.local();

            // 对讲负载不大，直接用本机
            return ResourceManagerResponse.allocated(token, AccessStreamRecource.talkback(local, announcer.getTalkback().getPort()));
        case "media":
        case "access_stream": // 申请一个 media server
            
            MediaServerMetadata mediaServer = clust.getLocation(token);
            if (null != mediaServer) {
                // 已经在线
                return ResourceManagerResponse.existing(token, AccessStreamRecource.media(mediaServer));
            } else if ((mediaServer = clust.allocInstance(token)) != null) {
                // 成功的，重新分配了一个 media server
                return ResourceManagerResponse.allocated(token, AccessStreamRecource.media(mediaServer));
            }

            // 没有 media 可用
            return ResourceManagerResponse.unavaliable(token, resource_type);

        case "storage":
        case "screenshot":
        case "algorithm":
        default:
            return ResourceManagerResponse.unavaliable(token, resource_type);
        }
    }
}
