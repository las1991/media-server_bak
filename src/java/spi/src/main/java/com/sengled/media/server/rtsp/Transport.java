package com.sengled.media.server.rtsp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/**
 * rtsp 协议的头信息中， Transoprt 属性的实例化对象
 *  
 * @author 陈修恒
 * @date 2016年4月15日
 */
public class Transport {
    public static final String RTP_AVP_TCP = "RTP/AVP/TCP";
    public static final String UNICAST = "unicast";
    public static final String INTERLEAVED = "interleaved";
    
    private String tranport = RTP_AVP_TCP;
    private String unicast = UNICAST;
    
    private int rtpChannel = -1;
    private int rtcpChannel = -1;
    private long ssrc;
    private String mode;
    
    
    public void setUnicast(String castMode) {
        this.unicast = castMode;
    }
    
    public void setTranport(String protocol) {
        this.tranport = protocol;
    }
    
    public String getTranport() {
        return tranport;
    }
    
    public String getUnicast() {
        return unicast;
    }
  
    
    public int getRtpChannel() {
        return rtpChannel;
    }

    public void setRtpChannel(int rtpChannel) {
        this.rtpChannel = rtpChannel;
    }

    public int getRtcpChannel() {
        return rtcpChannel;
    }

    public void setRtcpChannel(int rtcpChannel) {
        this.rtcpChannel = rtcpChannel;
    }

    public long getSsrc() {
        return ssrc;
    }

    public void setSsrc(long ssrc) {
        this.ssrc = ssrc;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public static Transport parse(String transport) {
        if (null == transport) {
            throw new IllegalArgumentException("Transport parse NULL");
        }
        
        Transport t = new Transport();
        String[] splits = StringUtils.split(transport,';');
        
        t.tranport = splits[0];
        t.unicast = splits[1];
        
        
        // RTP/AVP/TCP;unicast;interleaved=0-1;mode=record
        Matcher matcher;
        
        matcher = Pattern.compile("interleaved=(\\d+)-(\\d+)").matcher(transport);
        if (matcher.find()) {
            t.rtpChannel = Integer.parseInt(matcher.group(1));
            t.rtcpChannel = Integer.parseInt(matcher.group(2));
        }
        
        matcher = Pattern.compile("mode=([^;]+)").matcher(transport);
        if (matcher.find()) {
            t.mode = matcher.group(1);
        }
        
        matcher = Pattern.compile("ssrc=(\\d+)").matcher(transport);
        if (matcher.find()) {
            t.ssrc = Integer.parseInt(matcher.group(1));
        }
        
        return t;
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(tranport);
        buf.append(";").append(unicast);
        
        buf.append(";interleaved=").append(rtpChannel).append("-").append(rtcpChannel);
        if (null != mode) {
            buf.append(";mode=").append(mode);
        }
        if (ssrc > 0) {
            buf.append(";ssrc=").append(ssrc);
        }
        
        return buf.toString();
    }
}
