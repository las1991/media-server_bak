package com.sengled.media.server.rtsp.rtp;

import java.io.Closeable;
import java.io.IOException;
import org.apache.commons.lang.math.RandomUtils;
import org.mobicents.media.server.impl.rtcp.RtcpPacket;
import org.mobicents.media.server.impl.rtcp.RtcpPacketFactory;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import com.sengled.media.MediaCodec;
import com.sengled.media.clock.Rational;
import com.sengled.media.clock.SystemClock;
import com.sengled.media.server.DefaultStreamContext;
import com.sengled.media.server.MediaCodecExtra;
import com.sengled.media.server.rtsp.Transport;

public class RtpStreamContext<T extends MediaCodecExtra> extends DefaultStreamContext<T> implements Closeable, AutoCloseable {

    public static final String CNAME = "sengled-media-server-v3";

    private RtpStatistics statistics;

    private Transport transport;
    private String control;
    
    public RtpStreamContext(int streamIndex, Rational timeUnit, MediaCodec codec, T extra) {
        super(streamIndex, codec, extra, timeUnit);
        
        this.statistics = new RtpStatistics(new RtpClock(SystemClock.standard()), RandomUtils.nextLong(), CNAME);
    }
    
    @Override
    public void close() throws IOException {
        
    }
    
    public void setStatistics(RtpStatistics statistics) {
        this.statistics = statistics;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }
    
    public void setControl(String control) {
        this.control = control;
    }
    
    public String getControl() {
        return control;
    }
    


    public Rational getTimeUnit() {
        return timeUnit;
    }
    
    public int getPayloadType() {
        return getCodec().getRtpFormat();
    }

    public RtcpPacket bye() {
        return RtcpPacketFactory.buildBye(statistics);
    }
    

    public int getRtpChannel() {
        return transport.getRtpChannel();
    }
    
    public int getRtcpChannel() {
        return transport.getRtcpChannel();
    }
    
    public boolean isOK() {
        return null != transport && null != statistics;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{").append(getClass().getSimpleName());
        buf.append(", stream#").append(getStreamIndex());
        buf.append(", ").append(getCodec());
        buf.append("}");
        return buf.toString();
    }


    public RtpStatistics getStatistics() {
        return statistics;
    }
}
