package com.sengled.media.server.rtsp;

import com.sengled.media.MediaSource;
import com.sengled.media.event.EventType;

import java.net.SocketAddress;

public interface RtspSource extends MediaSource {

    @Override
    SocketAddress remoteAddress();

    @Override
    SocketAddress localAddress();

    @Override
    void postEvent(EventType event);

    @Override
    RtspSession getSession();

}
