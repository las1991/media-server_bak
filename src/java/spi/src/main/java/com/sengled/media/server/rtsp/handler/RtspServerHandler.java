package com.sengled.media.server.rtsp.handler;

import static io.netty.handler.codec.rtsp.RtspMethods.ANNOUNCE;
import static io.netty.handler.codec.rtsp.RtspMethods.DESCRIBE;
import static io.netty.handler.codec.rtsp.RtspMethods.GET_PARAMETER;
import static io.netty.handler.codec.rtsp.RtspMethods.PAUSE;
import static io.netty.handler.codec.rtsp.RtspMethods.PLAY;
import static io.netty.handler.codec.rtsp.RtspMethods.RECORD;
import static io.netty.handler.codec.rtsp.RtspMethods.REDIRECT;
import static io.netty.handler.codec.rtsp.RtspMethods.SETUP;
import static io.netty.handler.codec.rtsp.RtspMethods.SET_PARAMETER;
import static io.netty.handler.codec.rtsp.RtspMethods.TEARDOWN;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.slf4j.LoggerFactory;
import com.sengled.media.Version;
import com.sengled.media.server.RtspServlet;
import com.sengled.media.server.rtsp.IllegalFrame;
import com.sengled.media.server.rtsp.InterleavedFrame;
import com.sengled.media.server.rtsp.RtspServerContext;
import com.sengled.media.server.rtsp.servlet.AnnounceStreamServlet;
import com.sengled.media.server.rtsp.servlet.DescribeStreamServlet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.rtsp.RtspHeaders;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Rtsp 服务端协议栈
 *
 * @author 陈修恒
 * @date 2016年6月1日
 */
public class RtspServerHandler extends ChannelInboundHandlerAdapter {

    private static org.slf4j.Logger LOGGER = LoggerFactory
        .getLogger(RtspServerHandler.class);

    private final RtspServerContext serverContext;

    private ChannelHandlerContext channelContext;
    private RtspServlet servlet;
    private Set<HttpMethod> options;


    public RtspServerHandler(RtspServerContext rtspServerContext, HttpMethod[] options) {
        this.serverContext = rtspServerContext;
        this.options = new HashSet<>(Arrays.asList(options));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvt = (IdleStateEvent) evt;
            switch (idleEvt.state()) {
                case READER_IDLE:
                    if (servlet instanceof AnnounceStreamServlet) {
                        exceptionCaught(ctx, new TimeoutException(idleEvt.state() + " timeout"));
                    }
                    break;
                case WRITER_IDLE:
                    if (servlet instanceof DescribeStreamServlet) {
                        exceptionCaught(ctx, new TimeoutException(idleEvt.state() + " timeout"));
                    }
                    break;
                case ALL_IDLE:
                    // 既没有输入，也没有输出就断开
                    exceptionCaught(ctx, new TimeoutException(idleEvt.state() + " timeout"));
                    break;
                default:
                    LOGGER.debug("{} timeout", idleEvt.state());
                    break;
            }
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        channelContext = ctx;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        destroyChannelServlet();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Object error = null != servlet ? servlet : ctx.channel().remoteAddress();
        if (cause instanceof IOException) {
            LOGGER.info("{} {}", error, cause.getMessage());
        } else if (cause instanceof TimeoutException) {
            LOGGER.error("{} {}.", error, cause.getMessage());
        } else if (cause instanceof IndexOutOfBoundsException) {
            LOGGER.error("{}", error, cause);
        } else {
            LOGGER.error("{}", error, cause);
        }

        destroyChannelServlet();
    }

    private void destroyChannelServlet() {
        try {
            if (null != servlet) {
                servlet.destroy();
                servlet = null;
            }
        } finally {
            if (null != channelContext) {
                channelContext.close()
                    .addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future)
                            throws Exception {
                            channelContext = null;
                        }
                    });
            }
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        // Interleaved Frame
        if (msg instanceof InterleavedFrame) {
            InterleavedFrame frame = (InterleavedFrame) msg;
            try {
                if (null != servlet) {
                    servlet.channelRead(frame.retain());
                }
            } finally {
                ReferenceCountUtil.release(frame);
            }
            return;
        }

        // request
        else if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            HttpMethod method = request.getMethod();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} {}, from {}.", request.getMethod(), request.getUri(), ctx.channel().remoteAddress());
                LOGGER.debug("{}", request.content().toString(Charset.forName("UTF-8")));
            }
            
            final ChannelPromise promise = ctx.newPromise();
            final FullHttpResponse response = newResponse(request);
            try {
                if (RtspMethods.OPTIONS.equals(method)) {
                    response.headers().add(RtspHeaders.Names.PUBLIC, getOptionString());
                } else  if (TEARDOWN.equals(method)) {
                    response.setStatus(HttpResponseStatus.OK);
                    response.headers().add(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);

                    // 如果有 servelet， 则自动 teardown
                    RtspServlet thisServlet = this.servlet;
                    if (null != thisServlet) {
                        thisServlet.teardown(request, response);
                        promise.addListener(new GenericFutureListener<Future<? super Void>>() {
                            @Override
                            public void operationComplete(Future<? super Void> future) throws Exception {
                                thisServlet.destroy();
                                LOGGER.info("{} teardown", thisServlet);
                            }
                        });
                        this.servlet = null;
                    }
                }  else if (ANNOUNCE.equals(method) && null == servlet) {
                    servlet = new AnnounceStreamServlet(serverContext, ctx);
                    servlet.announce(request, response);
                } else if (DESCRIBE.equals(method) && null == servlet) {
                    servlet = new DescribeStreamServlet(serverContext, ctx);
                    servlet.describe(request, response);
                } else if (null != servlet){
                    if (GET_PARAMETER.equals(method)) {
                        servlet.getParameter(request, response);
                    } else if (PAUSE.equals(method)) {
                        servlet.pause(request, response);
                    } else if (PLAY.equals(method)) {
                        servlet.play(request, response);
                        LOGGER.info("{} {}, from {}.", request.getMethod(), request.getUri(), servlet);
                    } else if (RECORD.equals(method)) {
                        servlet.record(request, response);
                        LOGGER.info("{} {}, from {}.", request.getMethod(), request.getUri(), servlet);
                    } else if (REDIRECT.equals(method)) {
                        servlet.redirect(request, response);
                    } else if (SETUP.equals(method)) {
                        servlet.setup(request, response);
                    } else if (SET_PARAMETER.equals(method)) {
                        servlet.setParameter(request, response);
                    } else {
                        response.setStatus(HttpResponseStatus.CONFLICT);
                    }
                } else {
                   response.setStatus(HttpResponseStatus.BAD_REQUEST);
                }
                
            } catch (Exception ex) {
                response.setStatus(HttpResponseStatus.BAD_GATEWAY);
                final Object message = null != servlet ? servlet : "handshak failed";
                LOGGER.error("{}: {}", message, ex.getMessage(), ex);
            } finally {
                request.release();
            }

            // set session id
            if (null != servlet && null != servlet.getSession()) {
                response.headers().add(RtspHeaders.Names.SESSION, servlet.getSession().getSessionId());
            }
            // 链接是需要关闭的，但是没有 Connection 属性， 则自动补充上
            if (!isOK(response) && !isConnectionClose(response)) {
                response.headers().add(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);    
            }

            final Future<Void> f = ctx.writeAndFlush(response, promise);
            
            // 如果连接显示为关闭，则自动关闭
            if (isConnectionClose(response) || !isOK(response)) {
                f.addListener(new GenericFutureListener<Future<? super Void>>() {
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        ctx.close();
                    }
                });
                LOGGER.info("close {} for {} {}, {}.", ctx.channel().remoteAddress(), method, request.getUri(), response.getStatus());
            }
        } else if (msg instanceof IllegalFrame) {
            // 非法数据包
            try {
                IllegalFrame illegalFrame = (IllegalFrame) msg;
                ByteBuf in = illegalFrame.content();
                String dump = ByteBufUtil
                    .prettyHexDump(in, in.readerIndex(), Math.min(in.readableBytes(), 32));
                LOGGER.warn("illegal packet from {}.\r\n{}", servlet, dump);

                ctx.writeAndFlush(new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.BAD_REQUEST))
                    .addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future)
                            throws Exception {
                            // 关闭连接
                            destroyChannelServlet();
                            LOGGER.info("destroy for bad request");
                        }
                    });

            } finally {
                ReferenceCountUtil.release(msg);
            }
        }
        // others
        else {
            ctx.fireChannelRead(msg);
        }
    }

    private boolean isConnectionClose(final FullHttpResponse response) {
        return HttpHeaders.Values.CLOSE.equals(response.headers().get(HttpHeaders.Names.CONNECTION));
    }

    private boolean isOK(final FullHttpResponse response) {
        return response.getStatus() == HttpResponseStatus.OK
                || response.getStatus() == HttpResponseStatus.UNAUTHORIZED;
    }

    private FullHttpResponse newResponse(FullHttpRequest request) {
        final FullHttpResponse response;
        response = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
        response.headers().add(RtspHeaders.Names.DATE, new Timestamp(System.currentTimeMillis()));
        response.headers().add(RtspHeaders.Names.SERVER, Version.server());
        String cseq = request.headers().get(RtspHeaders.Names.CSEQ);
        if (null != cseq) {
            response.headers().add(RtspHeaders.Names.CSEQ, cseq);
        }

        return response;
    }

    private String getOptionString() {
        StringBuilder buf = new StringBuilder();
        for (HttpMethod option : options) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(option.name());
        }

        return buf.toString();
    }
}
