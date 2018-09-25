package com.sengled.media.server;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.util.ReferenceCountUtil;

/**
 * Frame Buffer
 * 
 * @author chenxh
 *
 */
public class MutableFramePacketBuffer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MutableFramePacketBuffer.class);

    private final int maxBufferSize; 
    private LinkedList<MutableFramePacket> frames = new LinkedList<>();
    
    public MutableFramePacketBuffer() {
        // GoP 最大可以是 50， 包括音频数据等 128 帧基本够了
        this(128);
    }
    
    public MutableFramePacketBuffer(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }
    
    public void add(MutableFramePacket frame) {
        frames.add(frame);
        
        // 自动删除
        while(frames.size() > maxBufferSize) {
            ReferenceCountUtil.release(frames.removeFirst());
        }
    }

    public int size() {
        return frames.size();
    }

    public List<MutableFramePacket> getLastGoP(int bufferSizeMillis) {
        List<MutableFramePacket> lastGoP = new ArrayList<>(64);
        
        for (MutableFramePacket mutableFramePacket : this.frames) {
            // 新的关键帧，把这个
            if(mutableFramePacket.isKeyFrame()) {
                lastGoP.clear();
            }

            // 组成了新的 GoP 了
            if (mutableFramePacket.isKeyFrame() || !lastGoP.isEmpty()) {
                lastGoP.add(mutableFramePacket);
            }
        }
        
        return lastGoP.stream().map(x -> x.duplicate().retain()).collect(Collectors.toList());
    };
    
    public void clear() {
        int numFrames = 0;
        while (!frames.isEmpty()) {
            ReferenceCountUtil.release(frames.removeFirst());
            numFrames++;
        }
        
        if (numFrames > 0) {
            LOGGER.debug("remove {} frames", numFrames);
        }
    }
    
    protected void finalize() throws Throwable {
        clear();
    }

}
