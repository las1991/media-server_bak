package com.sengled.media.server;

import org.apache.commons.lang.time.DateFormatUtils;
import com.sengled.media.FramePacket;
import com.sengled.media.MutableByteBufHolder;
import com.sengled.media.clock.Rational;
import com.sengled.media.clock.SystemClock;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

/**
 * 属性可变的 FramePacket
 *
 * pts 默认是 uts 时间
 *
 * @author chenxh
 */
public class MutableFramePacket extends MutableByteBufHolder implements FramePacket {
    private final int streamIndex;
    private final Rational timeUnit;

    private int flags;
    private long duration = -1;
    private long time;


    public static MutableFramePacket composite(FramePacket frame1, FramePacket frame2) {
        try {
            return new MutableFramePacket(Unpooled.wrappedBuffer(frame1.content().retain(), frame2.content().retain()), frame1);
        } finally {
            ReferenceCountUtil.release(frame1);
            ReferenceCountUtil.release(frame2);
        }
    }

    public MutableFramePacket(int streamIndex, Rational timeunit, ByteBuf data) {
        super(data);
        
        this.streamIndex = streamIndex;
        this.timeUnit = timeunit;
    }
    
    private MutableFramePacket(ByteBuf data, FramePacket frame) {
        super(data);
        this.streamIndex = frame.getStreamIndex();
        this.timeUnit = frame.getTimeUnit();
        this.time = frame.getTime();
        this.flags = frame.getFlags();
        this.duration = frame.getDuration();
    }

    @Override
    public MutableFramePacket duplicate() {
        return new MutableFramePacket(content().duplicate(), this);
    }

    @Override
    public MutableFramePacket copy() {
        return new MutableFramePacket(content().copy(), this);
    }

    @Override
    public MutableFramePacket retain() {
        super.retain();

        return this;
    }

    @Override
    public MutableFramePacket retain(int increment) {
        super.retain(increment);

        return this;
    }


    /* (non-Javadoc)
     * @see com.sengled.media.FramePacket#isKeyFrame()
     */
    @Override
    public boolean isKeyFrame() {
        return (flags & Flags.KEY_FRAME) > 0;
    }


    /* (non-Javadoc)
     * @see com.sengled.media.FramePacket#isDiscontinued()
     */
    @Override
    public boolean isDiscontinued() {
        return (flags & Flags.DISCONTINUE) > 0;
    }

    /* (non-Javadoc)
     * @see com.sengled.media.FramePacket#isImcomplete()
     */
    @Override
    public boolean isImcomplete() {
        return (flags & Flags.IMCOMPLETE_FRAME) > 0;
    }

    @Override
    public int getStreamIndex() {
        return streamIndex;
    }

    @Override
    public long getTime(Rational timeUnit) {
        return timeUnit.convert(time, this.timeUnit);
    }
    
    public long getDuration(Rational timeUnit) {
        return timeUnit.convert(duration, this.timeUnit);
    }

    @Override
    public int getFlags() {
        return flags;
    }


    @Override
    public boolean hasFlag(int flag) {
        return (flags & flag) > 0;
    }
    
    @Override
    public long getTime() {
        return time;
    }

    @Override
    public long getDuration() {
        return duration;
    }
    
    @Override
    public void setTime(long time, Rational unit) {
        this.time = this.timeUnit.convert(time, unit);
    }
    
    @Override
    public void setDuration(long duration, Rational unit) {
        this.duration = this.timeUnit.convert(duration, unit);
    }
    
    public void addFlag(int flag) {
        this.flags |= flag;
    }

    @Override
    public Rational getTimeUnit() {
        return timeUnit;
    }
    public void setFlags(int flags) {
        this.flags = flags;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setTime(long time) {
        this.time = time;
    }


    @Override
    public String toString() {
        long pts = getTime(Rational.MILLISECONDS);
        long duration = getDuration(Rational.MILLISECONDS);

        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append(", stream#").append(getStreamIndex());
        buf.append(", refCnt=").append(content().refCnt());
        buf.append(", duration=").append(duration).append("ms");
        buf.append(", pts=").append(DateFormatUtils.formatUTC(pts, "yyyy-MM-dd HH:mm:ss.SSS"));
        buf.append(", delay=").append(SystemClock.currentTimeMillis() - pts).append("ms");
        buf.append(", len=").append(content().readableBytes());
        buf.append(isKeyFrame() ? ", KEY" : "");
        buf.append(hasFlag(Flags.MD_FRAME) ? ", MD" : "");
        buf.append(hasFlag(Flags.HAS_PIR) ? ", hasPIR" : "");
        buf.append(hasFlag(Flags.PIR_FRAME) ? ", PIR" : "");
        buf.append(hasFlag(Flags.IMCOMPLETE_FRAME) ? ", imcomplete" : "");
        buf.append(hasFlag(Flags.DISCONTINUE) ? ", discontinued" : "");

        return buf.toString();
    }

}
