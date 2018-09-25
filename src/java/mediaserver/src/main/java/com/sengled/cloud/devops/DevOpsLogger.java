package com.sengled.cloud.devops;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.sengled.cloud.devops.DevOpsMetricService.Services;
import com.sengled.media.event.EventType;

/**
 * 按照运维的需求， 构造 json 格式的日志内容
 * 
 * @author chenxh
 */
public class DevOpsLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("DevOps");
    

    public static DevOpsLoggerWriter newWriter(String name) {
        return new DevOpsLoggerWriter(name);
    }

    
    public static final class DevOpsLoggerWriter {

        private final String source;

        private String token;
        
        
        /** 日志类型 */
        private String logType;
        
        /** 设备类型 */
        private String deviceType = "snap";

        /** 事件类型:online/offline/heatbeat */
        private EventType eventType;
        
        /** 链接类型:tcp/ui_ssl/bi_ssl */
        private String socketType;
        
        /** 设备的 IP  */
        private String deviceIp;

        /** 启动的服务 */
        private Services services;
        
        private DevOpsLoggerWriter(String source) {
            this.source = source;
        }
        
        public void write() {
            Asserts.notNull(token, "设备 token");
            Asserts.notNull(source, "产生日志的源服务");
            Asserts.notNull(logType, "本条日志类型");
            Asserts.notNull(eventType, "事件类型(online/offline/heartbeat)");
            Asserts.notNull(socketType, "链接类型(tcp/ui_ssl/bi_ssl)");
            Asserts.notNull(deviceIp, "设备公网ip");
            
            Map<String, Object> obj = new HashMap<>();
            obj.put("timestamp", System.currentTimeMillis());
            obj.put("uuid", RandomStringUtils.random(32, true, true));
            obj.put("token", token);
            obj.put("source", source);
            obj.put("log_type", logType);
            obj.put("type", deviceType);
            obj.put("event_type", eventType.toString());
            obj.put("link_type", socketType);
            obj.put("ip", deviceIp);
            
            if (null != services) {
                obj.put("motion", services.isMotion()?"on":"off");
                obj.put("object", services.isObject()?"on":"off");
                obj.put("storage", services.isStorage()?"on":"off");
                obj.put("screenshot", services.isScreenshot()?"on":"off");
            }

            String json = JSON.toJSONString(obj, SerializerFeature.IgnoreNonFieldGetter);
            LOGGER.info("{}", json);
        }
        
        public DevOpsLoggerWriter withToken(String token) {
            this.token = token;
            return this;
        }
        
        public DevOpsLoggerWriter withServices(Services services) {
            this.services = services;
            
            return this;
        }
        
        public DevOpsLoggerWriter withLogType(String logType) {
            this.logType = logType;
            
            return this;
        }
        
        public DevOpsLoggerWriter withDeviceType(String deviceType) {
            this.deviceType = deviceType;
            
            return this;
        }
        
        public DevOpsLoggerWriter withEventType(EventType eventType) {
            this.eventType = eventType;
            
            return this;
        }
        
        
        public DevOpsLoggerWriter withSSL(boolean useSSL) {
            this.socketType = useSSL ? "ui_ssl" : "tcp";
            
            return this;
        }
        
        public DevOpsLoggerWriter withDeviceIp(SocketAddress address) {
            if (address instanceof InetSocketAddress) {
                InetSocketAddress inet = (InetSocketAddress)address;
                
                if (null != inet.getHostString()) {
                    // 相对于  getHostName(), getHostString() 不会触发反向域名解析
                    this.deviceIp = inet.getHostString();
                } else {
                    this.deviceIp = inet.getAddress().getHostAddress();
                }
            }
          

            if (null == deviceIp) {
                deviceIp = "unknown";
            }
            
            return this;
        }
    }
    
    public static void main(String[] args) {
        DevOpsLoggerWriter logger = DevOpsLogger.newWriter("media");
        logger.token = "token";
        logger.logType = "online";
        logger.deviceType = "deviceType";
        logger.eventType = EventType.ONLINE;
        logger.socketType = "socketType";
        logger.deviceIp = "127.0.0.1";

        logger.write();
    }
}
