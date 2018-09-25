package com.sengled.media.server.rtsp.rtp.packetizer;

import java.util.concurrent.TimeUnit;
import org.mobicents.media.server.scheduler.Clock;
import com.sengled.media.clock.Rational;

/**
 * 手动更新的 RTP 时钟
 * @author chenxh
 */
class RtpMockClock implements Clock {
    private final Rational timeUnit;
    private long rtpTime;
    
    public RtpMockClock(Rational timeUnit) {
        super();
        this.timeUnit = timeUnit;
    }

    public void setRtpTime(long rtpTime, Rational unit) {
        this.rtpTime = this.timeUnit.convert(rtpTime, unit);
    }
    
    @Override
    public long getTime() {
        return getCurrentTime();
    }

    @Override
    public long getCurrentTime() {
        return Rational.MILLISECONDS.convert(rtpTime, timeUnit);
    }

    @Override
    public long getTime(TimeUnit timeUnit) {
        long cur = getCurrentTime();
        
        return timeUnit.convert(cur, TimeUnit.MILLISECONDS);
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

}
