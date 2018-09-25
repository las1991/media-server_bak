package com.sengled.media;

import com.sengled.media.event.EventType;
import com.sengled.media.server.rtsp.RtspServerContext;
import io.netty.util.concurrent.Future;

import java.net.SocketAddress;
import java.util.List;
import java.util.function.Function;

public interface MediaSource {

    MediaSession getSession();

    String getToken();

    void start();

    StreamContext<?>[] getStreamContexts();

    <T extends MediaSink> List<T> getMediaSinks(Class<T> class1);

    <T extends MediaSink> T getMediaSink(Class<T> sinkType);

    <T> Future<T> submit(Function<MediaDispatcher, T> task);

    void postEvent(EventType event);

    RtspServerContext getServerContext();

    /**
     * @return null if not known
     * @author chenxh
     */
    default SocketAddress remoteAddress() {
        return null;
    }

    default SocketAddress localAddress() {
        return null;
    }

    long getCreated();

    Future<Void> close();
}
