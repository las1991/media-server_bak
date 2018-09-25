package com.sengled.media.server.rtsp.rtp.handler;

import com.sengled.media.server.MutableFramePacket;
import com.sengled.media.server.rtsp.RtspServerContext;
import com.sengled.media.server.rtsp.RtspSession;
import com.sengled.media.server.rtsp.rtp.RtpPacket;
import com.sengled.media.server.rtsp.rtp.RtpPacketI;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.RecyclableArrayList;
import org.mobicents.media.server.impl.rtcp.RtcpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author las
 * @date 18-9-19
 */
public class RtpHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpHandler.class);

    private final RtspServerContext serverContext;
    private final RtspSession session;
    private final RtpDePacketizer<?>[] rtpDePacketizers;

    public RtpHandler(RtspServerContext serverContext, RtspSession session, RtpDePacketizer<?>[] rtpDePacketizers) {
        this.serverContext = serverContext;
        this.session = session;
        this.rtpDePacketizers = rtpDePacketizers;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RtpPacket) {
            RtpPacket rtpPacket = (RtpPacket) msg;
            try {
                //TODO 比较性能
                ctx.channel().eventLoop().submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        return null;
                    }
                });


                ctx.channel().eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {

                    }
                });

                for (RtpDePacketizer dePacketizer :
                        rtpDePacketizers) {
                    if (dePacketizer.getRtpChannel() == rtpPacket.getChannel()) {
                        onRtpReceive(dePacketizer, rtpPacket);
                    }
                }

            } catch (Exception e) {

            } finally {
                rtpPacket.release();
            }

        } else {
            //向下传播msg
            ctx.fireChannelRead(msg);
        }
    }

    private void onRtpReceive(RtpDePacketizer<?> dePacketizer, RtpPacket rtpPacket) {
        //TODO RtpPacket-->RtpPacketI
        RtpPacketI rtpPacketI = null;
        onRtpReceive(dePacketizer, rtpPacketI);
    }

    private void onRtpReceive(RtpDePacketizer<?> dePacketizer, RtpPacketI rtpPkt) {
        // 拆包后，转发出去
        RecyclableArrayList out = RecyclableArrayList.newInstance();
        try {
            dePacketizer.dePacket(rtpPkt.copyFast(), out);
            while (!out.isEmpty()) {
                final MutableFramePacket first = (MutableFramePacket) out.remove(0);
//                dispatcher.dispatch(first);
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

}
