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


    public void announce(FullHttpRequest request, FullHttpResponse response);

    public void describe(FullHttpRequest request, FullHttpResponse response);
    
    public void getParameter(FullHttpRequest request, FullHttpResponse response);

    public void pause(FullHttpRequest request, FullHttpResponse response);

    public void play(FullHttpRequest request, FullHttpResponse response);

    public void record(FullHttpRequest request, FullHttpResponse response);

    public void redirect(FullHttpRequest request, FullHttpResponse response);

    public void setup(FullHttpRequest request, FullHttpResponse response);

    public void teardown(FullHttpRequest request, FullHttpResponse response);

    public void setParameter(FullHttpRequest request, FullHttpResponse response);

	/**
	 * 处理 rtsp 上传的数据
	 * 
	 * @param frame
	 */
	public void channelRead(InterleavedFrame frame);
	
    /**
     * 关闭 Servlet
     */
    public void destroy();

    public RtspSession getSession();
}
