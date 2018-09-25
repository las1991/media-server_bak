package com.sengled.media.resourcemanager.webapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sengled.media.resourcemanager.webapp.apiv1.ServerInfo;

public class DeviceInfoResponse {
    public static final class DeviceInfo {
        private String token;
        private List<Map<String, Object>> resources;
        
        public DeviceInfo(String token) {
            this.token = token;
            this.resources = new ArrayList<>(3);
        }
        
        public boolean addServerInfo(String serverType, ServerInfo info) {
            if (null == info) {
                return false;
            }
            
            HashMap<String, Object> resource = new HashMap<>();
            
            resource.put("resource_type", serverType);
            resource.put("resource_addr", info.getHost());
            resource.put("resource_port", info.getPort());
            
            return resources.add(resource);
        }
        
        public String getToken() {
            return token;
        }
        
        public List<Map<String, Object>> getResources() {
            return resources;
        }
    }

    private String info;
    private String messageCode;
    private List<DeviceInfo> device_info = new ArrayList<>();
    public static DeviceInfoResponse ok(Iterator<DeviceInfo> deviceInfos) {
        DeviceInfoResponse response = new DeviceInfoResponse();
        response.info = "successful";
        response.messageCode = "200";
        
        while(deviceInfos.hasNext()) {
            response.device_info.add(deviceInfos.next());
        }
        
        return response;
    }

    
    public List<DeviceInfo> getDevice_info() {
        return device_info;
    }
    
    public String getInfo() {
        return info;
    }
    
    public String getMessageCode() {
        return messageCode;
    }
}
