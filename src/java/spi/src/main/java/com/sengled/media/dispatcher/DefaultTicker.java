package com.sengled.media.dispatcher;

import com.sengled.media.FramePacket;
import com.sengled.media.clock.MediaClock;
import com.sengled.media.clock.Rational;


public class DefaultTicker implements MediaClockTicker {
    private long diff; // 理论上， currentTime + diff 就是下一帧数据的 pts

    // 上一帧的 PTS
    private long lastPTS;
    private boolean isFirst = true;
    
    public DefaultTicker() {
        
    }

    @Override
    public final void tick(MediaClock clock, FramePacket frame) {
        long lastPTS = this.lastPTS;
        this.lastPTS = frame.getTime();
        
        if(isFirst) {
           isFirst = false;
           tickFirst(clock, frame); 
        } else {
           tickOne(clock, frame, lastPTS);
        }
    }

    protected void tickFirst(MediaClock clock, FramePacket frame) {
        
    }
    
    protected void tickOne(MediaClock clock, FramePacket frame, long lastPTS) {
        long amount = frame.getTime() - lastPTS;
        clock.tick(amount, frame.getTimeUnit());
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{").append(getClass().getSimpleName());
        buf.append(", diff=").append(diff).append("ms");
        buf.append("}");
        return buf.toString();
    }


}