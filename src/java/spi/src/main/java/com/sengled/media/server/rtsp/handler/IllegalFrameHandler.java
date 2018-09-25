package com.sengled.media.server.rtsp.handler;

import com.sengled.media.server.rtsp.IllegalFrame;
import com.sengled.media.server.rtsp.RtspSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.rtsp.RtspVersions;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author las
 * @date 18-9-25
 */
public class IllegalFrameHandler extends SimpleChannelInboundHandler<IllegalFrame> {

    private static Logger LOGGER = LoggerFactory.getLogger(IllegalFrameHandler.class);

    private final RtspSession session;

    public IllegalFrameHandler(RtspSession session) {
        this.session = session;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IllegalFrame msg) throws Exception {
        // 非法数据包
        try {
            IllegalFrame illegalFrame = (IllegalFrame) msg;
            ByteBuf in = illegalFrame.content();
            String dump = ByteBufUtil
                    .prettyHexDump(in, in.readerIndex(), Math.min(in.readableBytes(), 32));
            LOGGER.warn("illegal packet from {} <{}>.\r\n{}", ctx.channel().remoteAddress(), session.getUrl().toString(), dump);

            ctx.writeAndFlush(new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.BAD_REQUEST))
                    .addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future)
                                throws Exception {
                            // 关闭连接
                            LOGGER.info("destroy for bad request");
                            ctx.close();
                        }
                    });

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
