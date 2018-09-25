package com.sengled.media.dispatcher.strategy;

import java.util.List;
import com.sengled.media.FramePacket;

class UnknownMediaDispatchStrategy extends MediaDispatchStrategy{
    public UnknownMediaDispatchStrategy(String token) {
        super(token);
    }

    @Override
    public void read(FramePacket frame, List<Object> out) {
        out.add(frame);
    }

    @Override
    public void close() {
        
    }
}