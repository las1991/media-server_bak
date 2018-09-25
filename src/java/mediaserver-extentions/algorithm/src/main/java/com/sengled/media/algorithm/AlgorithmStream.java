package com.sengled.media.algorithm;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONObject;
import com.amazonaws.services.kinesis.producer.Attempt;
import com.amazonaws.services.kinesis.producer.UserRecordFailedException;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.sengled.media.clock.Rational;
import com.sengled.media.plugin.FrameStorer;
import com.sengled.media.plugin.config.storage.StorageConfig;
import com.sengled.media.server.MutableFramePacket;

public class AlgorithmStream implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlgorithmStream.class);

    private final AlgorithmSink sink;
    private final FrameStorer storer;
    private final String partitionKey;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final long createTime;

    private long lastModified = -1;

    public AlgorithmStream(AlgorithmSink sink, FrameStorer storer) {
        this.sink = sink;
        this.storer = storer;
        this.partitionKey = sink.getToken() + ",#" + RandomStringUtils.random(6, true, true);
        this.createTime = System.currentTimeMillis();
    }

    public void send(MutableFramePacket frame) {
        try {
            if (!closed.get()) {
                Action action = lastModified > 0 ? Action.exec : Action.open;
                builder(action)
                    .withStorageConfig(sink.getStorageConfig())
                    .withFrameStorer(storer)
                    .withPartitionKey(partitionKey)
                    .withTimestamp(frame.getTime(Rational.MILLISECONDS))
                    .save(frame);

                lastModified = frame.getTime(Rational.MILLISECONDS);
            }
        } finally {
            frame.release();
        }
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            builder(Action.close)
                .withStorageConfig(sink.getStorageConfig())
                .withFrameStorer(storer)
                .withPartitionKey(partitionKey)
                .withTimestamp(System.currentTimeMillis())
                .save(null);
        }
    }


    private TaskBuilder builder(Action action) {
        return new TaskBuilder(sink.getToken(), action);
    }

    private static class TaskBuilder {

        private final Action action;
        private final String token;

        private long timestamp = System.currentTimeMillis();
        private StorageConfig storageConfig;
        private FrameStorer storer;
        private String partitionKey;

        private TaskBuilder(String token, Action action) {
            this.action = action;
            this.token = token;
        }

        public TaskBuilder withStorageConfig(StorageConfig config) {
            this.storageConfig = config;
            return this;
        }

        public TaskBuilder withTimestamp(long timestamp) {
            this.timestamp = timestamp;

            return this;
        }


        public TaskBuilder withFrameStorer(FrameStorer storer) {
            this.storer = storer;

            return this;
        }

        public TaskBuilder withPartitionKey(String partitionKey) {
            this.partitionKey = partitionKey;

            return this;
        }

        public void save(MutableFramePacket frame) {
            if (null == partitionKey) {
                LOGGER.info("[{}] partition key is NULL", token);
                return;
            }

            JSONObject json = new JSONObject();
            json.put("utcDateTime",
                DateFormatUtils.formatUTC(timestamp, "yyyy-MM-dd HH:mm:ss.SSS"));


            // 没有配置，默认存 31 天
            if (null != storageConfig) {
                json.put("fileExpires", storageConfig.getFileExpires() * 24);
                json.put("timeZone", storageConfig.getTimeZone());
            } else {
                json.put("fileExpires", 31 * 24);
                json.put("timeZone", TimeZone.getDefault().getDisplayName());
            }

            // 构建消息内容
            ByteBuffer buffer;
            if (null != frame) {
                json.put("action", action.name());
                byte[] jsonBytes = json.toJSONString().getBytes();
                buffer = ByteBuffer
                    .allocate(3 + jsonBytes.length + frame.content().readableBytes());
                buffer.put((byte) '$');
                buffer.putShort((short) jsonBytes.length);
                buffer.put(jsonBytes);
                frame.content().readBytes(buffer);
            } else {
                json.put("action", Action.close); // frame == null, close auto.
                byte[] jsonBytes = json.toJSONString().getBytes();
                buffer = ByteBuffer.allocate(3 + jsonBytes.length);
                buffer.put((byte) '$');
                buffer.putShort((short) jsonBytes.length);
                buffer.put(jsonBytes);
            }
            buffer.flip();

            // 发送到 kinesis
            final long startAt = System.currentTimeMillis();
            final int length = buffer.remaining();
            storer.save(partitionKey, buffer, new FutureCallback<UserRecordResult>() {
                @Override
                public void onFailure(Throwable t) {
                    if (t instanceof UserRecordFailedException) {
                        Attempt last = Iterables
                            .getLast(((UserRecordFailedException) t).getResult().getAttempts());
                        LOGGER.error("Record failed to put - {} : {}", last.getErrorCode(),
                            last.getErrorMessage());
                    } else {
                        LOGGER.error("Exception during put {}", t);
                    }
                }

                @Override
                public void onSuccess(UserRecordResult result) {
                    switch (action) {
                        case open:
                            LOGGER.info("【{}】open at {}, delay {} ms", token, result.getShardId(),
                                System.currentTimeMillis() - startAt);
                            break;
                        case close:
                            LOGGER.info("【{}】stop at {}, delay {} ms", token, result.getShardId(),
                                System.currentTimeMillis() - startAt);
                            break;
                        default:
                            LOGGER
                                .debug("【{}】 sent {} bytes at {}, utcDateTime:[{}]", token, length,
                                    result.getShardId(), json.get("utcDateTime"));
                            break;
                    }
                }
            });
        }
    }
}
