package com.sengled.media.bootstrap.sqs;

import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Base64Utils;
import org.springframework.util.SerializationUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.sengled.media.bootstrap.AmazonAwsConfig;

/**
 * 用于处理 SQS 消息
 * @author chenxh
 */
public class SQSTemplate implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQSTemplate.class);

    @Autowired
    private AmazonAwsConfig config;

    @Value("${publisher_thread_count:3}")
    private Integer publisherThreadCount = 3; //发布消息线程数

    private ExecutorService executor = Executors.newFixedThreadPool(publisherThreadCount);

    private AmazonSQS sqsClient;

    @PreDestroy
    public void destroy(){
        sqsClient.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        AmazonSQS sqsClient = AmazonSQSClientBuilder
                                        .standard()
                                        .withRegion(config.getRegion())
                                        .withCredentials(config.getCredentialsProvider())
                                        .build();

        
        this.sqsClient = sqsClient;
        
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                final List<String> queueUrls = sqsClient.listQueues().getQueueUrls();
                for (String url : queueUrls) {
                    LOGGER.debug("{}", url);
                }
                
                LOGGER.info("SQS {} queue urls found", queueUrls.size());
            }
        }).start();
    }
    
    public String getQueryUrl(String queue) {
        String url = sqsClient.getQueueUrl(new GetQueueUrlRequest(queue)).getQueueUrl();
        return url;
    }
    
    public int readMessage(ReceiveMessageRequest request, MessageHandler handler) {
        List<Message> msgs = sqsClient.receiveMessage(request).getMessages();
        if (msgs.isEmpty()) {
            return 0;
        }
        
        try {
            doHandle(msgs, handler);
        } finally {
            doDelete(msgs, request.getQueueUrl());
        }
        
        return msgs.size();
    }

    private void doHandle(List<Message> msgs, MessageHandler handler) {
        for (Message message: msgs){
            try {
                LOGGER.debug("receive {}", message.getBody());

                String id = message.getMessageId();
                String serialized = "yes";
                Map<String, MessageAttributeValue> attrs = message.getMessageAttributes();
                if (null != attrs && null!=attrs.get("serialized")){
                    serialized = attrs.get("serialized").getStringValue();
                }
                LOGGER.debug("serialized:{}, attrs={}", serialized, attrs);

                if (serialized.equals("no")){
                    byte[]  result = Base64Utils.decodeFromString(message.getBody());
                    handler.handleByteMessage(id, result, attrs);
                }else {
                    Serializable obj = (Serializable)message.getBody();
                    handler.handleSerializedMessage(id, obj, attrs);
                }
            }catch (Exception e){
                LOGGER.error("Message press fail, {}", ExceptionUtils.getStackTrace(e));
            }
        }
    }

    private void doDelete(List<Message> msgs, String queueUrl) {
        List<DeleteMessageBatchRequestEntry> entries = new ArrayList<>();
        for (Message message: msgs){
            entries.add(new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()));
        }
        sqsClient.deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, entries));
    }

    public String publish(String queue,Serializable message){
        String encodedMessage;
        try {
            encodedMessage = Base64Utils.encodeToString(SerializationUtils.serialize(message));
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize message.", e);
        }

        checkState(encodedMessage.length() <= 256 * 1024);

        LOGGER.debug("Serialized Message: " + encodedMessage);

        String url = sqsClient.getQueueUrl(new GetQueueUrlRequest(queue)).getQueueUrl();
        SendMessageRequest request = new SendMessageRequest(url, encodedMessage);

        //request.withDelaySeconds(3);
        try {
            return sqsClient.sendMessage(request).getMessageId();
        } catch (AmazonServiceException e) {
            LOGGER.warn("Could not sent message to SQS queue: {}. Retrying.", url);
        }
        throw new RuntimeException("Exceeded  message not sent!");
    }

    public void publishOnBackend(final String queue, final Serializable message){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("publish message to sqs, queue={},content={}", queue,message);
                publish(queue, message);
                LOGGER.info("publish done");
            }
        });
    }
}
