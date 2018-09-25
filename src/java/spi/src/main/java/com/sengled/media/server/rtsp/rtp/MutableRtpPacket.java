package com.sengled.media.server.rtsp.rtp;

import com.sengled.media.MutableByteBufHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;

/**
 * 可修改的 RTP packet
 * 
 * @author chenxh
 */
public class MutableRtpPacket extends MutableByteBufHolder implements RtpPacketI {
    private int version = 2;
    private boolean marker;

    private int payloadType;
    private int seqNumber;

    private long time;
    private int flags;
    private long syncSource;
    
    public MutableRtpPacket(ByteBuf data) {
        super(data);
    }

    private MutableRtpPacket(ByteBuf data, MutableRtpPacket that) {
        super(data);
        this.time = that.time;
        this.seqNumber = that.seqNumber;
        this.marker = that.marker;
        this.flags = that.flags;
        this.version = that.version;
        this.syncSource = that.syncSource;
        this.payloadType = that.payloadType;
    }

    public MutableRtpPacket append(ByteBuf data) {
        try {
            CompositeByteBuf buf = (CompositeByteBuf)content();
            buf.addComponent(true, data.retain());
            
            return this;
        } finally {
            data.release();
        }
    }
    
    @Override
    public void setTime(long time) {
        this.time = time;
    }

    public void setSeqNumber(int seqNumber) {
        this.seqNumber = seqNumber;
    }

    public void setMarker(boolean marker) {
        this.marker = marker;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setSyncSource(long syncSource) {
        this.syncSource = syncSource;
    }

    public void setPayloadType(int payloadType) {
        this.payloadType = payloadType;
    }

    public long getTime() {
        return time;
    }

    public int getSeqNumber() {
        return seqNumber;
    }

    public boolean isMarker() {
        return marker;
    }

    public int getFlags() {
        return flags;
    }

    public int getVersion() {
        return version;
    }

    public long getSyncSource() {
        return syncSource;
    }

    @Override
    public boolean getMarker() {
        return marker;
    }

    @Override
    public int getPayloadType() {
        return payloadType;
    }

    @Override
    public int getPayloadLength() {
        return content().readableBytes();
    }
   
    @Override
    public MutableRtpPacket duplicate() {
        return new MutableRtpPacket(content().duplicate(), this);
    }

    @Override
    public MutableRtpPacket retain() {
        super.retain();
        return this;
    }

    @Override
    public MutableRtpPacket retain(int increment) {
        super.retain(increment);
        return this;
    }

    @Override
    public RtpPacketI copyFast() {
        return new MutableRtpPacket(content().duplicate().retain(), this);
    }

    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{").append(getClass().getSimpleName());
        buf.append(", refCnt=").append(refCnt());
        buf.append(", seq=").append(getSeqNumber());
        buf.append(", t=").append(getTime());
        buf.append(", size=").append(content().readableBytes());
        buf.append(", refCnt=").append(refCnt());
        buf.append(", ssrc=").append(getSyncSource());
        buf.append(", pType=").append(getPayloadType());
        buf.append(getMarker() ? " Marker" : "");
        buf.append("}");
        return buf.toString();
    }

}
