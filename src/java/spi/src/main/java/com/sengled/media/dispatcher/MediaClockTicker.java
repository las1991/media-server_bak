package com.sengled.media.dispatcher;

import com.sengled.media.FramePacket;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.MediaClock;
import com.sengled.media.dispatcher.audio.G711Ticker;
import com.sengled.media.dispatcher.audio.SpeexTicker;

public interface MediaClockTicker {

    void tick(MediaClock channelClock, FramePacket frame);

    static MediaClockTicker getClockTicker(StreamContext ctx) {
        MediaClockTicker ticker;

        switch (ctx.getCodec()) {
            case G711:
                ticker = new G711Ticker(ctx);
                break;
            case SPEEX:
                ticker = new SpeexTicker(ctx);
                break;
            case H264:
            default:
                ticker = new DefaultTicker();
                break;
        }

        return ticker;
    }

}