package com.sengled.media;

import com.sengled.media.clock.Rational;

import io.netty.buffer.ByteBufHolder;

/**
 * 一个视频帧
 * 
 * @author chenxh
 */
public interface FramePacket extends ByteBufHolder {
    public static final class Flags {
        public static final int RTP_MARKER = 1 << 0;
        public static final int KEY_FRAME = 1 << 1;
        public static final int MD_FRAME = 1 << 2;
        public static final int IMCOMPLETE_FRAME = 1 << 3;
        public static final int DISCONTINUE = 1 << 4;
        public static final int HAS_PIR = 1 << 5;
        public static final int PIR_FRAME = 1 << 6;
        private Flags(){}
    }

    
    int getStreamIndex();

    long getTime();
    
    Rational getTimeUnit();
    
    long getTime(Rational timeUnit);

    long getDuration();

    long getDuration(Rational timeUnit);
    
    int getFlags();
    
    /**
     * @see #RTP_MARKER
     * @see #KEY_FRAME
     * @see #MD_FRAME
     * @see #IMCOMPLETE_FRAME
     * @see #DISCONTINUE
     * @see #HAS_PIR
     * @see #PIR_FRAME
     * @param flag
     * @return
     */
    boolean hasFlag(int flag);

    boolean isDiscontinued();

    boolean isImcomplete();

    boolean isKeyFrame();

    @Override
    FramePacket copy();

    @Override
    FramePacket duplicate();

    @Override
    FramePacket retain();

    @Override
    FramePacket retain(int increment);

    void setTime(long time, Rational unit);

    void setDuration(long duration, Rational timeUnit);
}