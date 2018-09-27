package com.sengled.media.server.rtsp.servlet;

import com.sengled.media.server.RtspServlet;
import com.sengled.media.server.rtsp.Transport;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * 处理 RTSP 请求
 *
 * @author chenxh
 */
public abstract class RtspServletAdapter implements RtspServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(RtspServletAdapter.class);

    private final ChannelHandlerContext ctx;

    public RtspServletAdapter(ChannelHandlerContext ctx) {
        super();
        this.ctx = ctx;
    }

    public ByteBufAllocator alloc() {
        return ctx.alloc();
    }

    public void announce(FullHttpRequest request, FullHttpResponse response) {
        response.setStatus(HttpResponseStatus.NOT_IMPLEMENTED);
    }

    public void describe(FullHttpRequest request, FullHttpResponse response) {
        response.setStatus(HttpResponseStatus.NOT_IMPLEMENTED);
    }

    public void setup(FullHttpRequest request, FullHttpResponse response) {
        response.setStatus(HttpResponseStatus.NOT_IMPLEMENTED);
    }

    protected boolean isTransportSupported(Transport t) {
        return StringUtils.equals(Transport.RTP_AVP_TCP, t.getTranport())
//				&& StringUtils.equals(Transport.UNICAST, t.getUnicast())
                && t.getRtcpChannel() >= 0
                && t.getRtpChannel() >= 0;
    }


    public void play(FullHttpRequest request, FullHttpResponse response) {
        response.setStatus(HttpResponseStatus.NOT_IMPLEMENTED);
    }

    public void pause(FullHttpRequest request, FullHttpResponse response) {
        response.setStatus(HttpResponseStatus.NOT_IMPLEMENTED);
    }

    public void redirect(FullHttpRequest request, FullHttpResponse response) {
        response.setStatus(HttpResponseStatus.NOT_IMPLEMENTED);
    }

    public void record(FullHttpRequest request, FullHttpResponse response) {
        response.setStatus(HttpResponseStatus.NOT_IMPLEMENTED);
    }

    public void getParameter(FullHttpRequest request, FullHttpResponse response) {
        if (request.content().isReadable()) {
            String content = request.content().toString(Charset.defaultCharset());
            LOGGER.trace("GET_PARAMETER: {}", content);
        }
    }

    public void setParameter(FullHttpRequest request, FullHttpResponse response) {
        response.setStatus(HttpResponseStatus.NOT_IMPLEMENTED);
    }

    public void teardown(FullHttpRequest request, FullHttpResponse response) {
        response.headers().add(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }
}
