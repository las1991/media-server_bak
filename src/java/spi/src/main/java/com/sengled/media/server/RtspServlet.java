package com.sengled.media.server;

import com.sengled.media.server.rtsp.RtspSession;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.io.Closeable;

/**
 * 处理基于文本协议的流媒体服务
 *
 * @author chenxh
 */
public interface RtspServlet extends Closeable {

    void announce(FullHttpRequest request, FullHttpResponse response);

    void describe(FullHttpRequest request, FullHttpResponse response);

    void getParameter(FullHttpRequest request, FullHttpResponse response);

    void pause(FullHttpRequest request, FullHttpResponse response);

    void play(FullHttpRequest request, FullHttpResponse response);

    void record(FullHttpRequest request, FullHttpResponse response);

    void redirect(FullHttpRequest request, FullHttpResponse response);

    void setup(FullHttpRequest request, FullHttpResponse response);

    void teardown(FullHttpRequest request, FullHttpResponse response);

    void setParameter(FullHttpRequest request, FullHttpResponse response);

    RtspSession getSession();
}
