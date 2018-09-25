package com.sengled.media.plugin.sqs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.sengled.media.bootstrap.sqs.MessageHandler;
import com.sengled.media.bootstrap.sqs.SQSTemplate;

import io.netty.util.internal.SystemPropertyUtil;

/** 消息的消费者
 * 
 * @author chenxh
 */
public class SqsMessageConsumers {
    private final ScheduledExecutorService bossThreads;
    

    private int visibilityTimeoutInSeconds;

    private int requestTimeout;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SqsMessageConsumers.class);
    private static final SqsMessageConsumers INSTANCE = new  SqsMessageConsumers(1);
    
    public static final SqsMessageConsumers getInstance() {
        return INSTANCE;
    }
    
    private SqsMessageConsumers(int numBossThreads){
        bossThreads = Executors.newScheduledThreadPool(numBossThreads);
        visibilityTimeoutInSeconds = SystemPropertyUtil.getInt("sqs.message.visibility.timeout.seconds", 30);
        requestTimeout = SystemPropertyUtil.getInt("sqs.message.request.timeout.seconds", 3);
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                bossThreads.shutdown();
            }
        }, SqsMessageConsumers.class.getSimpleName() + "-shutdown-hook"));
    }
    
    public void consume(SQSTemplate template, String topic, JsonMessageHandler jsonMessageHandler) {

        // sqs 消息处理器
        final MessageHandler handler = new MessageHandler() {
            @Override
            public void handleSerializedMessage(final String messageId, Serializable data, Map<String, MessageAttributeValue> attrs) {
                LOGGER.info("[{}] {}", messageId, data);
                JSONObject result = JSON.parseObject((String) data);
                jsonMessageHandler.handle(result);
            }

            @Override
            public void handleByteMessage(final String messageId, byte[] bytes, Map<String, MessageAttributeValue> attrs) {
                String data = new String(bytes);
                LOGGER.info("[{}] {}", messageId, data);
                JSONObject result = JSON.parseObject(data);
                jsonMessageHandler.handle(result);
            }
        };
        
        // 订阅 sqs 消息
        final String url = template.getQueryUrl(topic);
        bossThreads.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    int read = 0;
                    do {
                        read = readMessage(template, url, handler);
                        if (read > 0) {
                            LOGGER.info("read {} sqs message", read);
                        }
                    } while(read > 0 && !Thread.interrupted());
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                
                LOGGER.debug("no sqs message read, just wait");
            }

        }, 200, 500, TimeUnit.MILLISECONDS);
        
        LOGGER.info("started thread receive sqs message");
    } 
    
    private int readMessage(final SQSTemplate template, final String url, final MessageHandler handler) {
        ReceiveMessageRequest request = new ReceiveMessageRequest(url);
        request.withMaxNumberOfMessages(10);
        request.withSdkRequestTimeout(1);
        request.withVisibilityTimeout(visibilityTimeoutInSeconds);
      
        request.withSdkRequestTimeout(requestTimeout * 1000);
        request.setMessageAttributeNames(new ArrayList<String>(Arrays.asList("All")));
        int read = template.readMessage(request, handler);
        return read;
    }
}
