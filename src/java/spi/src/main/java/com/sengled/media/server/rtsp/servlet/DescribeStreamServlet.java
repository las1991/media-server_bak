package com.sengled.media.server.rtsp.servlet;

import com.sengled.media.MediaCodec;
import com.sengled.media.MediaSource;
import com.sengled.media.StreamContext;
import com.sengled.media.server.MediaCodecExtra;
import com.sengled.media.server.rtsp.RtspServerContext;
import com.sengled.media.server.rtsp.RtspSession;
import com.sengled.media.server.rtsp.Transport;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpPacketizer;
import com.sengled.media.server.rtsp.rtp.packetizer.g711.G711RtpPacketizer;
import com.sengled.media.server.rtsp.rtp.packetizer.h264.AVCDecoderConfigurationRecord;
import com.sengled.media.server.rtsp.rtp.packetizer.h264.H264Packetizer;
import com.sengled.media.server.rtsp.rtp.packetizer.mpeg4.AudioSpecificConfig;
import com.sengled.media.server.rtsp.rtp.packetizer.mpeg4.Mpeg4RtpPacketizer;
import com.sengled.media.server.rtsp.rtp.packetizer.speex.SpeexRtpPacketizer;
import com.sengled.media.url.URLObject;
import gov.nist.javax.sdp.MediaDescriptionImpl;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.fields.ConnectionField;
import gov.nist.javax.sdp.fields.MediaField;
import gov.nist.javax.sdp.fields.OriginField;
import gov.nist.javax.sdp.fields.ProtoVersionField;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.rtsp.RtspHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 视频播放的 servlet
 *
 * @author chenxh
 */
public class DescribeStreamServlet extends RtspServletAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DescribeStreamServlet.class);

    private static final Pattern STREAMID = Pattern.compile("/streamid=(\\d+)$");

    private final RtspServerContext serverContext;
    private RtspSession session;

    private RtpPacketizer<?>[] rtpPacketizers;
    private RtspOverTcpSink sink;

    public DescribeStreamServlet(RtspServerContext serverContext, ChannelHandlerContext channelHandlerContext) {
        super(channelHandlerContext);
        this.serverContext = serverContext;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void describe(FullHttpRequest request, FullHttpResponse response) {
        URLObject url = URLObject.parse(request.getUri());
        this.session = new RtspSession(url);
        // 流不存在
        MediaSource source = serverContext.getMediaSource(this.session.getToken());
        if (null == source) {
            response.setStatus(HttpResponseStatus.NOT_FOUND);
            return;
        }

        // 拷贝音、视频流的配置

        this.rtpPacketizers = new RtpPacketizer<?>[source.getStreamContexts().length];
        StreamContext<?>[] src = source.getStreamContexts();
        for (int streamIndex = 0; streamIndex < rtpPacketizers.length; streamIndex++) {
            if (null != src[streamIndex]) {
                final MediaCodec codec = src[streamIndex].getCodec();
                switch (codec) {
                    case H264:
                        rtpPacketizers[streamIndex] = new H264Packetizer(streamIndex, (StreamContext<AVCDecoderConfigurationRecord>) src[streamIndex]);
                        rtpPacketizers[streamIndex].setControl("streamid=" + streamIndex);
                        break;
                    case G711:
                        rtpPacketizers[streamIndex] = new G711RtpPacketizer(streamIndex, (StreamContext<MediaCodecExtra>) src[streamIndex]);
                        rtpPacketizers[streamIndex].setControl("streamid=" + streamIndex);
                        break;
                    case AAC:
                        rtpPacketizers[streamIndex] = new Mpeg4RtpPacketizer(streamIndex, (StreamContext<AudioSpecificConfig>) src[streamIndex]);
                        rtpPacketizers[streamIndex].setControl("streamid=" + streamIndex);
                        break;
                    case SPEEX:
                        rtpPacketizers[streamIndex] = new SpeexRtpPacketizer(streamIndex, (StreamContext<MediaCodecExtra>) src[streamIndex]);
                        rtpPacketizers[streamIndex].setControl("streamid=" + streamIndex);
                        break;
                    default:
                        break;
                }
            }
        }


        /**
         * v=0
         * o=- 3823338519 3823338519 IN IP4 127.0.0.1
         * s=liveSengledIPC
         * i=LIVE555 Streaming Media
         * c=IN IP4 127.0.0.1
         * t=0 0
         * a=x-qt-text-nam:101.68.222.221
         * a=x-qt-text-inf:liveSengledIPC
         * a=x-qt-text-cmt:source application:LIVE555 Streaming Media
         * a=x-qt-text-aut:DarwinInjector
         * a=x-qt-text-cpy:
         * m=video 0 RTP/AVP 97
         * a=rtpmap:97 H264/90000
         * a=control:rtsp://101.68.222.221:554/ABE801222C0E9F722A0587480EE7E642.sdp/streamid=0
         * a=codecpts:1
         * m=audio 0 RTP/AVP 8
         * a=ptime:20
         * a=control:rtsp://101.68.222.221:554/ABE801222C0E9F722A0587480EE7E642.sdp/streamid=1
         */
        try {
            InetSocketAddress socketAddress = (InetSocketAddress) getCtx().channel().remoteAddress();
            final String netType = "IN";
            final String addressType = "IP4";
            final String address = socketAddress.getAddress().getHostAddress();
            // final String infoValue = "sengled media server v3.1";

            SessionDescription newSDP = new SessionDescriptionImpl();
            newSDP.setVersion(new ProtoVersionField());

            OriginField origin = new OriginField();
            origin.setUsername("-");
            origin.setAddress(address);
            origin.setAddressType(addressType);
            origin.setNettype(netType);
            origin.setSessionId(getSession().getSessionId());
            origin.setSessionVersion(getSession().getLastModified());
            newSDP.setOrigin(origin);

            Connection conn = new ConnectionField();
            conn.setAddress(address);
            conn.setAddressType(addressType);
            conn.setNetworkType(netType);
            newSDP.setConnection(conn);


            List<MediaDescription> mdList;
            mdList = Arrays.asList(rtpPacketizers)
                    .stream()
                    .filter(x -> null != x)
                    .map(new Function<RtpPacketizer<?>, MediaDescription>() {
                        @Override
                        public MediaDescription apply(RtpPacketizer<?> t) {
                            MediaDescription md = new MediaDescriptionImpl();
                            try {
                                // a=rtpmap:97 MPEG4-GENERIC/44100/2
                                // a=fmtp:97
                                // profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3;
                                // config=121056E500
                                final MediaCodec codec = ((RtpPacketizer<? extends MediaCodecExtra>) t).getCodec();

                                // m=audio 0 RTP/AVP 97
                                final MediaField media = new MediaField();
                                media.setMedia(codec.isVideo() ? "video" : (codec.isAudio() ? "audio" : "text"));
                                media.setProto("RTP/AVP");
                                media.setFormats(new Vector<>(Arrays.asList(codec.getRtpFormat())));
                                md.setMedia(media);

                                // 把媒体描述信息落地到 SDP 中
                                (t).readMediaDescription(md);

                                // control:stream=1
                                md.setAttribute("control", t.getControl());
                            } catch (SdpException e) {
                                LOGGER.error("Fail set md attr for {}", e.getMessage(), e);
                            }
                            return md;
                        }
                    }).collect(Collectors.toList());
            newSDP.setMediaDescriptions(new Vector<>(mdList));


            final String sdpString = newSDP.toString();
            response.content().writeBytes(sdpString.getBytes());
            response.headers().set(RtspHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(RtspHeaders.Names.CONTENT_TYPE, "application/sdp");


            LOGGER.debug("[{}-{}] {}", this.session.getToken(), this.session.getSessionId(), sdpString);
        } catch (SdpException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void setup(FullHttpRequest request, FullHttpResponse response) {
        Matcher matcher = STREAMID.matcher(request.getUri());
        if (!matcher.find()) { // stream 不合法
            response.setStatus(HttpResponseStatus.BAD_REQUEST);
            return;
        }

        // 只支持 RTP over TCP 
        String exceptTransport = request.headers().get(RtspHeaders.Names.TRANSPORT);
        Transport t = Transport.parse(exceptTransport);
        if (!isTransportSupported(t)) {
            response.setStatus(HttpResponseStatus.NOT_IMPLEMENTED);
            return;
        }

        int streamIndex = Integer.parseInt(matcher.group(1));
        response.headers().add(RtspHeaders.Names.TRANSPORT, t.toString());

        if (null != rtpPacketizers[streamIndex]) {
            rtpPacketizers[streamIndex].setTransport(t);
        }
    }

    @Override
    public void play(FullHttpRequest request, FullHttpResponse response) {
        if (null != sink) { // 对讲是通过  PLAY 来保活的
            LOGGER.warn("[{}] played again", session.getToken());
            return;
        }

        // 流不存在
        MediaSource source = serverContext.getMediaSource(session.getToken());
        if (null == source) {
            response.setStatus(HttpResponseStatus.NOT_FOUND);
            return;
        }

        sink = new RtspOverTcpSink(getCtx(), source, rtpPacketizers);
        sink.start();
    }

    @Override
    public RtspSession getSession() {
        return session;
    }


    @Override
    public void close() throws IOException {
        if (null != sink) {
            sink.close();
            sink = null;
        }
    }

    @Override
    public String toString() {
        return "DescribeStreamServlet{" +
                "serverContext=" + serverContext +
                ", session=" + session +
                ", rtpPacketizers=" + Arrays.toString(rtpPacketizers) +
                ", sink=" + sink +
                '}';
    }
}
