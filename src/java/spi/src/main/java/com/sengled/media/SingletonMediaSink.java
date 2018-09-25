package com.sengled.media;

import java.io.IOException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * 单例的 MeidaSink
 * @author chenxh
 */
public abstract class SingletonMediaSink implements MediaSink {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingletonMediaSink.class);
    
    protected final MediaSource source;
    
    protected SingletonMediaSink(MediaSource source) {
        this.source  = source;
    }
    
    public String getToken() {
        return source.getToken();
    }
    
    @Override
    public final void start() {
        MediaSink sink = this;
        source.submit(new Function<MediaDispatcher, Boolean>() {
            @Override
            public Boolean apply(MediaDispatcher dispatcher) {
                //初始化成功后加入
                return dispatcher.getMediaSink(sink.getClass()) == null &&
                        dispatcher.addMediaSink(sink);
            }
        }).addListener(new GenericFutureListener<Future<Boolean>>() {
            @Override
            public void operationComplete(Future<Boolean> future) throws Exception {
                if (future.isSuccess() && future.get()) {
                    onStarted();
                } else {
                    close();
                }
            }
        });
    }

    protected void onStarted() {
        LOGGER.info("[{}] {} registed", source.getToken(), this);
    }

    protected void onClosed() throws IOException {
        LOGGER.info("[{}] {} closed", source.getToken(), this);
    }
    @Override
    public final Future<Void> close() {
        MediaSink sink = this;
        return source.submit(new Function<MediaDispatcher, Void>() {
            @Override
            public Void apply(MediaDispatcher t) {
                t.removeMediaSink(sink);
                return null;
            }
        }).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                onClosed();
            }
        });
    }

}
