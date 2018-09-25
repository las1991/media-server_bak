package com.sengled.media.server.rtsp.servlet;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.function.Function;
import javax.sdp.SdpException;
import org.apache.commons.io.IOUtils;
import org.mobicents.media.server.impl.rtcp.RtcpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.MediaDispatcher;
import com.sengled.media.MediaSink;
import com.sengled.media.MediaSource;
import com.sengled.media.StreamContext;
import com.sengled.media.dispatcher.DefaultMediaDispatcher;
import com.sengled.media.event.EventType;
import com.sengled.media.event.SessionEvent;
import com.sengled.media.server.MutableFramePacket;
import com.sengled.media.server.rtsp.InterleavedFrame;
import com.sengled.media.server.rtsp.RtspServerContext;
import com.sengled.media.server.rtsp.RtspSession;
import com.sengled.media.server.rtsp.RtspSource;
import com.sengled.media.server.rtsp.Transport;
import com.sengled.media.server.rtsp.rtp.RTP;
import com.sengled.media.server.rtsp.rtp.RtpPacketI;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;
import com.sengled.media.url.URLObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.RecyclableArrayList;

/**
 * 设备通过 RTSP over TCP 上传的流
 *
 * @author chenxh
 */
public class RtspOverTcpSource extends ChannelHandlerContextHolder implements RtspSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtspOverTcpSource.class);

    private final RtspServerContext serverContext;
    private final RtspSession session;
    private final RtpDePacketizer<?>[] rtpDePacketizers;

    private final MediaDispatcher dispatcher;

    public RtspOverTcpSource(ChannelHandlerContext ctx, RtspServerContext serverContext, URLObject url, RtpDePacketizer<?>[] rtpDePacketizers) throws SdpException {
        super(ctx);

        this.serverContext = serverContext;
        this.session = new RtspSession(url);
        this.rtpDePacketizers = rtpDePacketizers;
        this.dispatcher = new DefaultMediaDispatcher(this, session.getToken(), rtpDePacketizers);
    }

    @Override
    public String getToken() {
        return session.getToken();
    }

    @Override
    public long getCreated() {
        return session.getLastModified();
    }

    @Override
    public void start() {
        String token = session.getToken();

        this.serverContext.addMediaSource(token, this);
    }

    @Override
    public RtspSession getSession() {
        return session;
    }

    @Override
    protected void beforeClose() throws IOException {
        // 关闭 session 和 tcp 连接
        try {
            for (int i = 0; null != rtpDePacketizers && i < rtpDePacketizers.length; i++) {
                RtpDePacketizer<?> depacketizer = rtpDePacketizers[i];
                try {
                    if (!isOK(depacketizer) || !isWritable()) {
                        continue;
                    }

                    // SNAP firmware 2.0 发送 BYE 包会造成退流进程崩溃
                    /** 发送 RTCP BYE
                    final int rtcpChannel = depacketizer.getRtcpChannel();
                    final ByteBuf rtcp = RTP.encode(depacketizer.bye());
                    write(new InterleavedFrame(rtcpChannel, rtcp)).addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future) throws Exception {
                            LOGGER.info("[{}] sent BYE success ? {}", getToken(), future.isSuccess());
                        }
                    });
                    */
                } finally {
                    IOUtils.closeQuietly(depacketizer);
                }
            }
            
            flush(); // 刷新内容
        } catch (Exception e) {
            LOGGER.error("Fail close for {}", e.getMessage(), e);
        } finally {
            dispatcher.close();  // 关闭客户端
        }
    }

    @Override
    protected void onClosed(ChannelPromise promise) throws IOException {
        // 从上下文中移除
        MediaSource source = this;
        promise.addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                serverContext.removeMediaSource(getToken(), source);
            }
        });
        
    }

    @Override
    public void postEvent(EventType event) {
        serverContext.postEvent(SessionEvent.newEvent(this, event));
    }

    @Override
    public StreamContext<?>[] getStreamContexts() {
        StreamContext<?>[] rtpStreams = new StreamContext[this.rtpDePacketizers.length];
        for (int i = 0; i < rtpStreams.length; i++) {
            rtpStreams[i] = rtpDePacketizers[i];
        }
        return rtpStreams;
    }

    @Override
    public <T extends MediaSink> List<T> getMediaSinks(Class<T> sinkType) {
        return null != dispatcher ? dispatcher.getMediaSinks(sinkType) : Collections.emptyList();
    }

    @Override
    public <T extends MediaSink> T getMediaSink(Class<T> sinkType) {
        return null != dispatcher ? dispatcher.getMediaSink(sinkType) : null;
    }


    @Override
    public RtspServerContext getServerContext() {
        return serverContext;
    }

    @Override
    public <T> Future<T> submit(Function<MediaDispatcher, T> task) {
        return eventLoop().submit(new Callable<T>() {
            @Override
            public T call() throws Exception {
                try {
                    return task.apply(dispatcher);
                } catch (Exception e) {
                    LOGGER.error("channel:{} submit fail for {}", channel(), e.getMessage(), e);
                    throw e;
                }
            }
        });
    }


    public void onInterleavedFrameReceived(InterleavedFrame frame) {
        try {
            if (isClosed()) {
                throw new IllegalStateException(this + " is closed");
            }

            for (RtpDePacketizer<?> rtpStream : rtpDePacketizers) {
                if (!isOK(rtpStream)) {
                    continue;
                }

                // rtp
                if (rtpStream.getRtpChannel() == frame.channel()) {
                    RtpPacketI rtp = null;
                    try {
                        rtp = RTP.decode(frame.content().retain());
                        onRtpReceive(rtpStream, rtp);
                    } finally {
                        ReferenceCountUtil.release(rtp);
                    }
                    return;
                }

                // rtcp
                if (rtpStream.getRtcpChannel() == frame.channel()) {
                    RtcpPacket rtcpPacket = new RtcpPacket();
                    int offset = 0;
                    byte[] bytes = frame.toBytes();
                    while (offset < bytes.length) {
                        offset = rtcpPacket.decode(bytes, offset);
                    }

                    onRtcpReceive(rtpStream, rtcpPacket);
                    return;
                }
            }
        } finally {
            frame.release();
        }
    }


    private boolean isOK(RtpDePacketizer<?> rtpStream) {
        return null != rtpStream && rtpStream.isOK();
    }

    private void onRtpReceive(RtpDePacketizer<?> dePacketizer, RtpPacketI rtpPkt) {
        // 拆包后，转发出去
        RecyclableArrayList out = RecyclableArrayList.newInstance();
        try {
            dePacketizer.dePacket(rtpPkt.copyFast(), out);
            while (!out.isEmpty()) {
                final MutableFramePacket first = (MutableFramePacket) out.remove(0);
                dispatcher.dispatch(first);
            }
        } finally {
            recycle(out);
        }
    }

    private void onRtcpReceive(RtpDePacketizer<?> rtpStream, RtcpPacket rtcpPacket) {
        rtpStream.dePacket(rtcpPacket);
    }

    public void setTransport(int streamIndex, Transport t) {
        rtpDePacketizers[streamIndex].setTransport(t);
    }

}
