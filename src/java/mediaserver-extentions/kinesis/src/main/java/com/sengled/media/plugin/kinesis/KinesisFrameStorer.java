package com.sengled.media.plugin.kinesis;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.sengled.media.plugin.FrameStorer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created by las on 2017/3/6.
 */
public class KinesisFrameStorer implements FrameStorer {

    private static Logger LOGGER = LoggerFactory.getLogger(KinesisFrameStorer.class);

    private final String streamName;
    private final KinesisProducer producer;

    public KinesisFrameStorer(KinesisProducer producer, String streamName) {
        this.streamName = streamName;
        this.producer = producer;
    }


    @Override
    public void save(final String token, final ByteBuffer content, FutureCallback<UserRecordResult> callback) {
        ListenableFuture<UserRecordResult> future = producer.addUserRecord(streamName, token, content);
        Futures.addCallback(future, callback);
    }

}