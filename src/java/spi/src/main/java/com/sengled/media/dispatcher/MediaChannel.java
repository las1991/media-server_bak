package com.sengled.media.dispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.FramePacket;
import com.sengled.media.MediaCodec;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.MediaClock;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.MutableFramePacket;
import io.netty.util.ReferenceCountUtil;

public class MediaChannel {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaChannel.class);
    
    private final String token;
    private final MediaClock channelClock;
    private final MediaClockTicker ticker;
    private final MediaCodec codec;
    private final int streamIndex;

    
    public MediaChannel(String token, StreamContext ctx) {
        super();
        this.token = token;
        this.ticker = MediaClockTicker.getClockTicker(ctx);
        this.streamIndex = ctx.getStreamIndex();
        this.codec = ctx.getCodec();
        
        final Rational timeUnit = ctx.getTimeUnit();
        if (ctx.getCodec().isVideo()) {
            this.channelClock = new MediaClock(timeUnit);
        } else {
            this.channelClock = new MediaClock(timeUnit, 0, 1000);
        }
    }

   public void feed(FramePacket frame, List<Object> out) {
        try {
            ticker.tick(channelClock, frame);
            
            FramePacket read = frame.copy();
            read.setTime(channelClock.currentTime(), channelClock.getTimeUnit());
            read.setDuration(channelClock.getTicked(), channelClock.getTimeUnit());
            out.add(read);
            
            LOGGER.debug("[{}] {}", token, read);
        } finally {
            ReferenceCountUtil.release(frame);
        }
    }
   
    public long currentTimeMillis() {
        return channelClock.currentTimeMillis();
    }

    public boolean adjustToTime(long newTimeMillis) {
        long adjustedDuration = newTimeMillis - channelClock.currentTimeMillis();
        
        boolean adjusted = (adjustedDuration > 0) && channelClock.adjustMillis(adjustedDuration);
        if (adjusted) {
            LOGGER.debug("{} stream#{} adjusted {}ms, cur = {}", 
                    token,
                    streamIndex, 
                    adjustedDuration, 
                    DateFormatUtils.format(channelClock.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss.SSS"));
        } else {
            LOGGER.debug("{} stream#{} ignore adjust {}ms, cur = {}", 
                    token,
                    streamIndex, 
                    adjustedDuration, 
                    DateFormatUtils.format(channelClock.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss.SSS"));
        }
        return adjusted;
    }
    
 

    public int getStreamIndex() {
        return streamIndex;
    }
    
    public MediaCodec getCodec() {
        return codec;
    }
}