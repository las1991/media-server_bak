package com.sengled.media.server.rtsp.rtp.packetizer.mpeg4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.sengled.media.FramePacket;
import com.sengled.media.MediaCodec;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.rtsp.rtp.RtpPacketI;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpPacketizer;
import io.netty.buffer.ByteBuf;

public class Mpeg4RtpPacketizer extends RtpPacketizer<AudioSpecificConfig> {
    private long rtpTime = 0;
    private int maxFramesInOneRtpPacket = 3;
    private ArrayList<FramePacket> buffer = new ArrayList<>();
    
    private Mpeg4PacketConfig config = new Mpeg4PacketConfig();;

    public Mpeg4RtpPacketizer(int streamIndex, StreamContext<AudioSpecificConfig> streamContext) {
        super(streamIndex, Rational.$44_100, streamContext);
    }
    
    public Mpeg4RtpPacketizer(int streamIndex, AudioSpecificConfig extra, Rational timeUnit) {
        super(streamIndex, timeUnit, MediaCodec.AAC, extra);
    }

  

    @Override
    protected void packet0(FramePacket frame, List<Object> out) {
        if (config.sizeLength + config.indexLength != 16) {
            throw new IllegalArgumentException();
        }
        if (config.sizeLength + config.indexDeltaLength != 16) {
            throw new IllegalArgumentException();
        }
        
        // 空间不够， 直接 flush
        if (estimatePacketSize() + 2 + frame.content().readableBytes() > maxPacketSize) {
            flush(out);
        }
        
        // 缓存
        this.rtpTime = super.getRtpTime();
        buffer.add(frame.retain());
        
        // 已经足够 3 个音频包
        if (buffer.size() > maxFramesInOneRtpPacket) {
            flush(out);
        }
    }
    
    private int estimatePacketSize() {
        int size = 2;
        
        for (FramePacket framePacket : buffer) {
            size += 2 + framePacket.content().readableBytes();
        }
        
        return size;
    }
    
    public void flush(List<Object> out) {
        if (buffer.isEmpty()) {
            return;
        }

        RtpPacketI rtp = newRTP();
        try {
            rtp.setTime(rtpTime);
            final ByteBuf content = rtp.content();
            
            // AU-headers-length
            int auHeaderSize = 2 * buffer.size() ;
            int auHeaderLength = 0; // (config.sizeLength + config.indexLength) + (buffer.size() - 1) * (config.sizeLength + config.indexDeltaLength);
            content.writeShort((auHeaderSize << (16 - config.sizeLength)) | auHeaderLength);

            // AU-header
            for (int i = 0; i < buffer.size(); i++) {
                FramePacket packet = buffer.get(i);
                int headerSection = 0;
                headerSection |= (packet.content().readableBytes()) << ( 0 == i ? config.indexLength : 
config.indexDeltaLength);
                headerSection |= 0; // index or delta
                content.writeShort(headerSection);
            }

            // data
            for (int i = 0; i < buffer.size(); i++) {
                content.writeBytes(buffer.get(i).content());
            }
        } finally {
            out.add(rtp);
            release(buffer);
        }
    }

    @Override
    public final void release() throws IOException {
        release(buffer);
    }
    
    @Override
    protected void finalize() throws Throwable {
        release(buffer);
    }
    
    public void setMaxFramesInOneRtpPacket(int maxFramesInOneRtpPacket) {
        this.maxFramesInOneRtpPacket = maxFramesInOneRtpPacket;
    }
    
    public Mpeg4PacketConfig getConfig() {
        return config;
    }
}
