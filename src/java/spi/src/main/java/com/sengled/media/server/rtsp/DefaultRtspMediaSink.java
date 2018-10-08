package com.sengled.media.server.rtsp;

import com.sengled.media.MediaDispatcher;
import com.sengled.media.MediaSession;
import com.sengled.media.MediaSink;
import com.sengled.media.StreamContext;
import com.sengled.media.dispatcher.DefaultMediaDispatcher;
import com.sengled.media.event.EventType;
import com.sengled.media.event.SessionEvent;
import com.sengled.media.server.MutableFramePacket;
import com.sengled.media.server.rtsp.rtcp.RtcpPacket;
import com.sengled.media.server.rtsp.rtp.InterleavedRtpPacket;
import com.sengled.media.server.rtsp.rtp.MutableRtpPacket;
import com.sengled.media.server.rtsp.rtp.RtpPacketI;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.RecyclableArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * @author las
 * @date 18-9-25
 */
public class DefaultRtspMediaSink implements RtspMediaSink {

    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultRtspMediaSink.class);

    private final RtspServerContext serverContext;
    private final RtspSession session;
    private final RtpDePacketizer<?>[] rtpDePacketizers;
    private final ChannelHandlerContext ctx;

    private final MediaDispatcher dispatcher;

    public DefaultRtspMediaSink(RtspServerContext serverContext, RtspSession session, RtpDePacketizer<?>[] rtpDePacketizers, ChannelHandlerContext context) {
        this.serverContext = serverContext;
        this.session = session;
        this.rtpDePacketizers = rtpDePacketizers;
        this.ctx = context;
        this.dispatcher = new DefaultMediaDispatcher(this, session.getToken(), rtpDePacketizers);
    }

    @Override
    public void onRtp(InterleavedRtpPacket rtpPacket) {
        try {
            for (RtpDePacketizer dePacketizer :
                    rtpDePacketizers) {
                if (!(null != dePacketizer && dePacketizer.isOK())) {
                    continue;
                }
                if (dePacketizer.getRtpChannel() == rtpPacket.getChannel()) {
                    onRtpReceive(dePacketizer, rtpPacket);
                }
            }
        } finally {
            rtpPacket.release();
        }
    }

    @Override
    public void onRtcp(RtcpPacket rtcpPacket) {
        //TODO deal rtcp
//        onRtcpReceive();
    }


    private void onRtpReceive(RtpDePacketizer<?> dePacketizer, InterleavedRtpPacket rtpPacket) {
        MutableRtpPacket rtp = new MutableRtpPacket(rtpPacket.payload());
        rtp.setFlags(rtpPacket.profile());
        rtp.setMarker(rtpPacket.marker());
        rtp.setPayloadType(rtpPacket.payloadType());
        rtp.setSeqNumber(rtpPacket.seqNumber());
        rtp.setSyncSource(rtpPacket.SSRC());
        rtp.setTime(rtpPacket.time());
        rtp.setVersion(rtpPacket.version());
        onRtpReceive(dePacketizer, rtp);
    }

    private void onRtpReceive(RtpDePacketizer<?> dePacketizer, RtpPacketI rtpPkt) {
        // 拆包后，转发出去
        RecyclableArrayList out = RecyclableArrayList.newInstance();
        try {
            dePacketizer.dePacket(rtpPkt.duplicate().retain(), out);
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

    private void recycle(List list) {
        if (list instanceof ArrayList) {
            while (null != list && !list.isEmpty()) {
                int lastIndex = list.size() - 1;
                ReferenceCountUtil.release(list.remove(lastIndex));
            }
        } else {
            while (null != list && !list.isEmpty()) {
                ReferenceCountUtil.release(list.remove(0));
            }
        }
    }

    @Override
    public MediaSession getSession() {
        return session;
    }

    @Override
    public String getToken() {
        return session.getToken();
    }

    @Override
    public void start() {
        final String token = session.getToken();
        this.serverContext.addMediaSource(token, this);
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
    public <T> Future<T> submit(Function<MediaDispatcher, T> task) {
        return ctx.executor().submit(new Callable<T>() {
            @Override
            public T call() throws Exception {
                try {
                    return task.apply(dispatcher);
                } catch (Exception e) {
                    LOGGER.error("channel:{} submit fail for {}", ctx.channel(), e.getMessage(), e);
                    throw e;
                }
            }
        });
    }

    @Override
    public void postEvent(EventType event) {
        serverContext.postEvent(SessionEvent.newEvent(this, event));
    }

    @Override
    public RtspServerContext getServerContext() {
        return serverContext;
    }

    @Override
    public long getCreated() {
        return session.getLastModified();
    }

    @Override
    public Future<Void> close() {
        // 从上下文中移除
        try {
            boolean success = serverContext.removeMediaSource(getToken(), this);
            if (success) {
                dispatcher.close();

                for (RtpDePacketizer dePacketizer :
                        rtpDePacketizers) {
                    dePacketizer.close();
                }
            }
        } catch (IOException e) {

        }
        return ctx.newSucceededFuture();
    }
}
