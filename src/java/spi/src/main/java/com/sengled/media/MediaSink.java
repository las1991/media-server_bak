package com.sengled.media;

import java.io.IOException;
import java.util.List;
import com.sengled.media.server.MutableFramePacket;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;

public interface MediaSink {
    public void start();

    public Future<Void> close();
    

    default public void setup(List<MutableFramePacket> frames) throws IOException {
        try {
            for (MutableFramePacket item : frames) {
                onFrame(item.retain()); // 可能会发生异常
            }
        } finally {
            for (MutableFramePacket item : frames) {
                ReferenceCountUtil.release(item);
            }    
        }
    }

    public void onFrame(MutableFramePacket frame) throws IOException;

    default public boolean isFromStorage() {
        return false;
    }

    default public boolean isClosed() {
        return false;
    }
}
