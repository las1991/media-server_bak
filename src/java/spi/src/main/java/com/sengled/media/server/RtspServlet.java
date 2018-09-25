package com.sengled.media.server;

import com.sengled.media.server.rtsp.InterleavedFrame;
import com.sengled.media.server.rtsp.RtspSession;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * 处理基于文本协议的流媒体服务
 *
 * @author chenxh
 */
public interface RtspServlet {

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

    /**
     * 处理 rtsp 上传的数据
     *
     * @param frame
     */
    void channelRead(InterleavedFrame frame);

    /**
     * 关闭 Servlet
     */
    void destroy();

    RtspSession getSession();
}
