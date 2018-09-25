package com.sengled.media.dispatcher.audio;

import com.sengled.media.FramePacket;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.MediaClock;
import com.sengled.media.clock.Rational;
import com.sengled.media.clock.SystemClock;
import com.sengled.media.dispatcher.DefaultTicker;


public class G711Ticker extends DefaultTicker {
    private final Rational timeUnit;
    
    public G711Ticker(StreamContext ctx) {
        super();
        this.timeUnit = Rational.$8_000;
    }
    
    @Override
    protected void tickOne(MediaClock clock, FramePacket frame, long lastPTS) {
        long amount = timeUnit.toMillis(frame.getTime() - lastPTS);
        long duration = timeUnit.toMillis(frame.content().readableBytes());
        
        if (amount > duration && amount + clock.currentTimeMillis() > SystemClock.currentTimeMillis()) {
            amount = duration; // 时间增量大于时长，且增量看起来是非法的
        } else if (amount < 0) {
            amount = duration; // 时间必须是递增的
        }

        clock.tick(amount, Rational.MILLISECONDS);
    }
}