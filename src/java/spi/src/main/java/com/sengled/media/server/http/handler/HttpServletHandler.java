package com.sengled.media.server.http.handler;


import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import com.sengled.media.MediaSink;
import com.sengled.media.MediaSource;
import com.sengled.media.Version;
import com.sengled.media.server.http.FlvStreamingMediaSink;
import com.sengled.media.server.rtsp.RtspServerContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Rtsp 服务端协议栈
 * 
 * @author 陈修恒
 * @date 2016年6月1日
 */
public class HttpServletHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HttpServletHandler.class);

    private final RtspServerContext serverContext;
    
    private ChannelHandlerContext channelContext;
    private FlvStreamingMediaSink sink;

    public HttpServletHandler(RtspServerContext rtspServerContext) {
        this.serverContext = rtspServerContext;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvt = (IdleStateEvent)evt;
            
            // http 只有下行数据
            if (idleEvt.state() == IdleState.WRITER_IDLE) {
                exceptionCaught(ctx, new TimeoutException(idleEvt.state() + " timeout"));
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
        Object error = ctx.channel().remoteAddress();
        if(cause instanceof IOException) {
            LOGGER.info("{} {}", error, cause.getMessage());
        } else if (cause  instanceof TimeoutException) {
            LOGGER.error("{} {}.", error, cause.getMessage());
        } else if (cause instanceof IndexOutOfBoundsException) {
            LOGGER.error("{}", error, cause);
        } else {
            LOGGER.error("{}:{}, {}.", cause.getClass().getSimpleName(), cause.getMessage(), error);
            LOGGER.debug("{}", cause.getMessage(), cause);
        }

        destroyChannelServlet();
    }

	private void destroyChannelServlet() {
	    try {
	        if (null != this.sink) {
	            sink.close();
	        }
	    } finally {
	        if (null != channelContext) {
	            channelContext.close();
	        }
	    }
	}

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        final String uri = StringUtils.split(request.getUri(), '?')[0];
        final String queryString = getQueryString(request);
        
        // 只支持 GET 请求
        if (null == sink && HttpMethod.GET.equals(request.getMethod()) && "flv".equals(FilenameUtils.getExtension(uri))) {
            String token = FilenameUtils.getBaseName(uri);
            doGet(ctx, request, token, queryString);
            return;
        }
        
        forbidden(ctx, HttpResponseStatus.FORBIDDEN);
    }

    private String getQueryString(FullHttpRequest request) {
        String url = request.getUri();
        int index = StringUtils.indexOf(url, '?');
        
        String queryString = null;
        if (index > 0 && index + 1 < url.length()) {
            queryString = url.substring(index + 1);
        }
        return queryString;
    }

    
    private void doGet(ChannelHandlerContext ctx, FullHttpRequest request, String token, String queryString) {
        MediaSource source = serverContext.getMediaSource(token);
        if (null == source) {
            forbidden(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        // 保证录像只有一个录像链接
        synchronized (source) {
            if (StringUtils.contains(queryString, FlvStreamingMediaSink.FROM_MEDIA_STORAGE)) {
                List<MediaSink> sinks = source.getMediaSinks(MediaSink.class);
                for (MediaSink sink : sinks) {
                    if (sink.isFromStorage()) {
                        LOGGER.warn("[{}] forbidden storage request from {}", token, ctx.channel().remoteAddress());
                        forbidden(ctx, HttpResponseStatus.TOO_MANY_REQUESTS);
                        return;
                    }
                }

                LOGGER.info("[{}] accepted singleton=true from {}", token, ctx.channel().remoteAddress());
            }
            
            // 响应请求
            HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
            response.headers().add(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
            response.headers().add(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
            response.headers().add(HttpHeaders.Names.CONTENT_TYPE, "video/x-flv");
            response.headers().add(HttpHeaders.Names.CACHE_CONTROL, "no-cache, must-revalidate");
            response.headers().add(HttpHeaders.Names.PRAGMA, "no-cache");
            response.headers().add(HttpHeaders.Names.SERVER, Version.server());
            ctx.writeAndFlush(response);
            
            // 开始直播
            sink = new FlvStreamingMediaSink(ctx, source, queryString);
            sink.start();
        }
    }


    private void forbidden(ChannelHandlerContext ctx, HttpResponseStatus status) {
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().add(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        response.headers().add(HttpHeaders.Names.DATE, new Date());
        response.headers().add(HttpHeaders.Names.SERVER, Version.server());
        
        if (status == HttpResponseStatus.NOT_FOUND) {
            response.content().writeBytes("stream not found ".getBytes());
        } else if (status == HttpResponseStatus.FORBIDDEN) {
            response.content().writeBytes("forbidden".getBytes());
        }
        
        final GenericFutureListener<Future<? super Void>> listener = new GenericFutureListener<Future<? super Void>>() {
           public void operationComplete(Future<? super Void> future) throws Exception {
               ctx.close();
           }; 
        };
        
        HttpHeaders.setContentLength(response, response.content().readableBytes());
        ctx.writeAndFlush(response).addListener(listener);
    }


    
}
