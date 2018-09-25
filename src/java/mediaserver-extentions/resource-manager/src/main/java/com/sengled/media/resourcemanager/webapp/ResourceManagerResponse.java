package com.sengled.media.resourcemanager.webapp;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 响应请求的标准格式
 * 
 * @author chenxh
 */
public class ResourceManagerResponse {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerResponse.class);
    
    private String info;
    
    private String messageCode;
    
    private String token;
    
    private List<ServiceResource> resource_info;
    
    // fresh：本次返回的可用资源， existing：服务已分配
    private String allocate;  
    
    /**
     * 
     * @param token
     * @return
     */
    public static ResourceManagerResponse error(String token, String serverType) {
        ResourceManagerResponse resource;
        resource = new ResourceManagerResponse();
        resource.info = "parameter error";
        resource.messageCode = "21001";
        resource.allocate = "fresh";
        resource.token = token;
        
        if (isLogable(serverType)) {
            LOGGER.error("[{}] cant get {} server for {}", token, serverType, resource.info);
        }
        return resource;
    }
    
    /**
     * 没有可用的服务
     * 
     * @param token
     * @return
     */
    public static ResourceManagerResponse unavaliable(String token, String serverType) {
        ResourceManagerResponse resource;
        resource = new ResourceManagerResponse();
        resource.info = "no resources available";
        resource.messageCode = "21002";
        resource.allocate = "fresh";
        resource.token = token;
        

        if (isLogable(serverType)) {
            LOGGER.error("[{}] cant get {} server for {}", token, serverType, resource.info);
        }
        return resource;
    }

    /**
     * 灯还没有上线
     * 
     * @param token
     * @return
     */
    public static ResourceManagerResponse offline(String token, String serverType) {
        ResourceManagerResponse resource;
        resource = new ResourceManagerResponse();
        resource.info = "streaming media server offline";
        resource.messageCode = "21003";
        resource.allocate = "fresh";
        resource.token = token;
        

        if (isLogable(serverType)) {
            LOGGER.warn("[{}] cant get {} server for {}", token, serverType, resource.info);
        }
        return resource;
    }

    public static ResourceManagerResponse allocated(String token, ServiceResource... servers) {
        ResourceManagerResponse resource;
        resource = new ResourceManagerResponse();
        resource.info = "successful";
        resource.messageCode = "200";
        resource.allocate = "fresh";
        resource.token = token;
        
        resource.resource_info = Arrays.asList(servers);
        
        LOGGER.debug("[{}] fresh {} ", token, Arrays.toString(servers));
        return resource;
    }
    
    public static ResourceManagerResponse existing(String token, ServiceResource... servers) {
        ResourceManagerResponse resource;
        resource = new ResourceManagerResponse();
        resource.info = "successful";
        resource.messageCode = "200";
        resource.allocate = "existing";
        resource.token = token;
        
        resource.resource_info = Arrays.asList(servers);

        LOGGER.debug("[{}] existing {} ", token, Arrays.toString(servers));
        return resource;
    }
    
    private static boolean isLogable(String serverType) {
        return !"algorithm".equals(serverType) 
                && !"screenshot".equals(serverType);
    }
    
    public String getAllocate() {
        return allocate;
    }
    
    public String getInfo() {
        return info;
    }
    
    public String getMessageCode() {
        return messageCode;
    }
    
    public List<ServiceResource> getResource_info() {
        return resource_info;
    }
    
    
    public String getToken() {
        return token;
    }
}
