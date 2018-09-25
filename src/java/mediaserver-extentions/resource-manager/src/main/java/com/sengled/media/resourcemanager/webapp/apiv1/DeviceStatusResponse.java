package com.sengled.media.resourcemanager.webapp.apiv1;

import java.util.ArrayList;
import java.util.List;

/**
 * device_status 对应的接口
 * 
 * @author chenxh
 */
public class DeviceStatusResponse {
    private String info;
    private String messageCode;
    private List<DeviceInfo> device_info = new ArrayList<>();

    public static DeviceStatusResponse ok(List<DeviceInfo> result) {
        DeviceStatusResponse response = new DeviceStatusResponse();
        response.info = "successful";
        response.messageCode = "200";
        

        response.device_info.addAll(result);

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

    public static final class DeviceInfo {
        private String status = "online";
        private String token;
        private int resource_port;
        private String resource_type;
        private String resource_addr;

        public String getResource_addr() {
            return resource_addr;
        }

        public int getResource_port() {
            return resource_port;
        }

        public String getResource_type() {
            return resource_type;
        }

        public String getStatus() {
            return status;
        }

        public String getToken() {
            return token;
        }

        public static DeviceInfo offline(String resourceType, String token) {
            DeviceInfo info = new DeviceInfo();
            info.token = token;
            info.resource_type = resourceType;
            info.status = "offline";

            return info;
        }

        public static DeviceInfo online(String resourceType, String token, String serverHost, int port) {
            DeviceInfo info = new DeviceInfo();
            info.token = token;
            info.resource_type = resourceType;
            info.status = "online";
            info.resource_addr = serverHost;
            info.resource_port = port;
            return info;
        }
    }


}
