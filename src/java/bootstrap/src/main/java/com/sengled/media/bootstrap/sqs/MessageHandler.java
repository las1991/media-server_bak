package com.sengled.media.bootstrap.sqs;

import java.io.Serializable;
import java.util.Map;

import com.amazonaws.services.sqs.model.MessageAttributeValue;

/**
 * 用来处理 SQS 消息的回调函数
 * 
 * @author chenxh
 */
public interface MessageHandler {
    public void handleByteMessage (String messageId, byte[] data, Map<String, MessageAttributeValue> attrs);
    

    public void handleSerializedMessage(String messageId, Serializable data, Map<String, MessageAttributeValue> attrs);
}
