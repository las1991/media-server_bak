package com.sengled.media.resourcemanager.webapp.apiv1;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.sengled.media.clust.MediaServerClust;
import com.sengled.media.clust.server.MediaServerMetadata;
import com.sengled.media.resourcemanager.webapp.apiv1.DeviceStatusResponse.DeviceInfo;

@RestController
public class DeviceStatusController {
    @Autowired
    private MediaServerClust clust;
    
    /**
     * 请求算法、截图、存储服务会调用本接口
     * @param resource_type
     * @param token
     * @return
     */
    @RequestMapping(path = { "/{resource_type}/device_status/", "/{resource_type}/device_status" })
    @ResponseBody
    public DeviceStatusResponse getDeviceStatus(@PathVariable("resource_type") String resource_type,  @RequestParam("token") String tokenArray) {
        List<String> tokens = Arrays.asList(StringUtils.split(tokenArray, ","));
        
        // 默认都是不在线
        List<DeviceInfo> results = tokens.stream().map(token -> DeviceInfo.offline(resource_type, token)).collect(Collectors.toList());
        switch(resource_type) {
        case "access_stream":
            results =
            tokens.stream().map(token -> {
                MediaServerMetadata metadata = clust.getLocation(token);
                
                DeviceInfo info = null;
               if (null != metadata) {
                   info = DeviceInfo.online(resource_type, token, metadata.getPublicHost(), metadata.getTcpPort());
               } else {
                   info = DeviceInfo.offline(resource_type, token);
               }
               
               return info;
            }).collect(Collectors.toList());
            
            break;
        }
        
        return DeviceStatusResponse.ok(results);
    }
}
