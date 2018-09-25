package com.sengled.media.server.rtsp.codec;

import static io.netty.handler.codec.http.HttpConstants.COLON;
import static io.netty.handler.codec.http.HttpConstants.CR;
import static io.netty.handler.codec.http.HttpConstants.LF;
import static io.netty.handler.codec.http.HttpConstants.SP;

import java.util.Map.Entry;

import com.sengled.media.server.rtsp.InterleavedFrame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;

public class RtspObjectEncoder extends MessageToByteEncoder<Object>{
    /**
     * Constant for CRLF.
     */
    private static final byte[] CRLF = {CR, LF};
    
    private static final byte[] HEADER_SEPERATOR = { COLON, SP };

    @Override
    public boolean acceptOutboundMessage(final Object msg)
           throws Exception {
        return (msg instanceof FullHttpMessage) || (msg instanceof InterleavedFrame);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx,
                          Object msg,
                          ByteBuf out) throws Exception {
        if (msg instanceof FullHttpMessage) {
            FullHttpMessage http = (FullHttpMessage)msg;
             outputHttpMessage(http, out);
        } else {
            final InterleavedFrame src = (InterleavedFrame)msg;
            outputFrame(src, out);
        }
    }

    private void outputFrame(InterleavedFrame frame, ByteBuf out) {
        out.writeByte('$');
        out.writeByte(frame.channel());
        out.writeShort(frame.content().readableBytes());
        out.writeBytes(frame.content());
        
    }

    private void outputHttpMessage(FullHttpMessage http, ByteBuf out) throws Exception {
        encodeInitialLine(http, out);
        encodeHeaders(http.headers(), out);
        out.writeByte(CR);
        out.writeByte(LF);
        
        encodeContent(http, out);
    }
    


    protected void encodeInitialLine(final HttpMessage message, final ByteBuf buf)
           throws Exception {
        if (message instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) message;
            HttpHeaders.encodeAscii(request.getMethod().toString(), buf);
            buf.writeByte(SP);
            buf.writeBytes(request.getUri().getBytes(CharsetUtil.UTF_8));
            buf.writeByte(SP);
            HttpHeaders.encodeAscii(request.getProtocolVersion().toString(), buf);
            buf.writeBytes(CRLF);
        } else if (message instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) message;
            HttpHeaders.encodeAscii(response.getProtocolVersion().toString(),
                                    buf);
            buf.writeByte(SP);
            buf.writeBytes(String.valueOf(response.getStatus().code())
                                 .getBytes(CharsetUtil.US_ASCII));
            buf.writeByte(SP);
            HttpHeaders.encodeAscii(String.valueOf(response.getStatus().reasonPhrase()),
                                    buf);
            buf.writeBytes(CRLF);
        } else {
            throw new UnsupportedMessageTypeException("Unsupported type "
                                + StringUtil.simpleClassName(message));
        }
    }

    private void encodeHeaders(HttpHeaders headers, ByteBuf out) {
        for (Entry<String, String> header: headers) {
            encode(header.getKey(), header.getValue(), out);
        }
    }
    

    private static void encode(CharSequence key, CharSequence value, ByteBuf out) {
        if (!HttpHeaders.encodeAscii(key, out)) {
            out.writeBytes(HEADER_SEPERATOR);
        }
        
        if (!HttpHeaders.encodeAscii(value, out)) {
            out.writeBytes(CRLF);
        }
    }



    private void encodeContent(FullHttpMessage msg,
                               ByteBuf out) {
        out.writeBytes(msg.content());
    }
}
