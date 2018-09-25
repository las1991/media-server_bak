
package com.sengled.cloud.devops.mdflags;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.sengled.media.server.rtsp.RtspServerContext;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
@ConditionalOnClass(DelegatingWebSocketMessageBrokerConfiguration.class)
public class MdFlagsWebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        StompWebSocketEndpointRegistration regist = registry.addEndpoint("/mdflags");
        regist.addInterceptors(new HttpSessionHandshakeInterceptor());
    }
    
    @Bean
    public SubProtocolHandler defaultSubProtocolHandler(SubProtocolWebSocketHandler handler, RtspServerContext context) {
        final MdFlagsHandler defaultProtocolHandler = new MdFlagsHandler(context);
        handler.setDefaultProtocolHandler(defaultProtocolHandler);
        
        return defaultProtocolHandler;
    }
}

