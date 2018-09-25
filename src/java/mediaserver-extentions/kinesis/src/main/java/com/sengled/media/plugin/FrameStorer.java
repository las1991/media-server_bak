package com.sengled.media.plugin;

import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.google.common.util.concurrent.FutureCallback;

import java.nio.ByteBuffer;

/**
 * Created by las on 2017/3/6.
 */
public interface FrameStorer {
    public void save(String token, ByteBuffer content, FutureCallback<UserRecordResult> callback);
}
