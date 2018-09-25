package com.sengled.media.server.rtsp.rtp;

import java.text.ParseException;
import java.util.Arrays;

import javax.sdp.SessionDescription;

import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;
import com.sengled.media.server.rtsp.sdp.SdpParser;

public class TestMediaDescriptionParserFactory {
    public static void main(String[] args) throws ParseException {
        StringBuilder buf = new StringBuilder();
        buf.append("v=0").append("\r\n");
        buf.append("o=- 2892550915 2892550915 IN IP4 127.0.0.1").append("\r\n");
        buf.append("s=liveSengledIPC").append("\r\n");
        buf.append("i=LIVE555 Streaming Media").append("\r\n");
        buf.append("c=IN IP4 127.0.0.1").append("\r\n");
        buf.append("t=0 0").append("\r\n");
        buf.append("a=x-qt-text-nam:101.68.222.220").append("\r\n");
        buf.append("a=x-qt-text-inf:liveSengledIPC").append("\r\n");
        buf.append("a=x-qt-text-cmt:source application:LIVE555 Streaming Media").append("\r\n");
        buf.append("a=x-qt-text-aut:DarwinInjector").append("\r\n");
        buf.append("a=x-qt-text-cpy:").append("\r\n");
        buf.append("m=video 0 RTP/AVP 97").append("\r\n");
        buf.append("a=rtpmap:97 H264/90000").append("\r\n");
        buf.append("a=control:trackID=1").append("\r\n");
        buf.append("a=codecpts:1").append("\r\n");
        buf.append("m=audio 0 RTP/AVP 8").append("\r\n");
        buf.append("a=ptime:20").append("\r\n");
        buf.append("a=control:trackID=2").append("\r\n");
        
        SessionDescription sd = SdpParser.parse(buf.toString());
        RtpDePacketizer[] deS = MediaDescriptionParserFactory.parse(sd);

        System.out.println(Arrays.toString(deS));
        
    }
}
