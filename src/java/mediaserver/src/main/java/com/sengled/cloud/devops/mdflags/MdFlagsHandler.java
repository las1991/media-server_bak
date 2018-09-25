package com.sengled.cloud.devops.mdflags;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SubProtocolHandler;

import com.sengled.media.MediaSource;
import com.sengled.media.server.rtsp.RtspServerContext;

/**
 * 用来监控视频帧 md 标记
 * 
 * @author chenxh
 */
public class MdFlagsHandler implements SubProtocolHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MdFlagsHandler.class);
    
    private RtspServerContext context;
    public MdFlagsHandler(RtspServerContext context) {
        this.context = context;
    }



    @Override
    public List<String> getSupportedProtocols() {
        return Arrays.asList();
    }

    @Override
    public void handleMessageFromClient(WebSocketSession session, WebSocketMessage<?> message,
            MessageChannel outputChannel) throws Exception {
        LOGGER.info("recv {}", message);
        TextMessage text = asTextMessage(message);
        if (null == text) {
            return;
        }
        
        // 把上一个 session 关闭
        MdFlagsMediaSink.close(session);

        // 如果只是 close 动作，则不继续了
        String token = text.getPayload();
        if("close".equals(token)) {
            return;
        }

        MediaSource source = context.getMediaSource(token);
        if (null != source) {
            new MdFlagsMediaSink(source, session).start();
        } else {
            session.sendMessage(new TextMessage("stream [" + token + "] NOT found"));
        }
    }



    private TextMessage asTextMessage(WebSocketMessage<?> message) {
        TextMessage text = null;
        if(message instanceof TextMessage) {
            text = (TextMessage)message;
        }
        return text;
    }

    @Override
    public void handleMessageToClient(WebSocketSession session, Message<?> message) throws Exception {
        LOGGER.info("{}", message);
    }

    @Override
    public String resolveSessionId(Message<?> message) {
        return null;
    }

    @Override
    public void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus, MessageChannel outputChannel)
            throws Exception {
        MdFlagsMediaSink.close(session);
    }

}
