package com.sengled.media.server.rtsp.rtp.packetizer.h264;

import java.util.List;
import com.sengled.media.FramePacket;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.rtsp.rtp.MutableRtpPacket;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpPacketizer;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.RecyclableArrayList;

public class H264Packetizer extends RtpPacketizer<AVCDecoderConfigurationRecord> {


    public H264Packetizer(int streamIndex, StreamContext<AVCDecoderConfigurationRecord> src) {
        super(streamIndex, Rational.$90_000, src);
    }
    
    protected void packet0(FramePacket frame, List<Object> out) {
        ByteBuf buf = frame.content();
        RecyclableArrayList nalList = newArrayList();
        try {
            H264.split(buf.retain(), nalList);
            
            ByteBuf nal;
            
            int aggregatedNalSize = 1; // nal type = 24
            int aggregatedPacketLength = 0;
            for (int nalIndex = 0; nalIndex < nalList.size(); nalIndex++) {
                nal = (ByteBuf)nalList.get(nalIndex);
                if (aggregatedNalSize + nal.readableBytes() + 2 < maxPacketSize) {
                    aggregatedPacketLength = nalIndex + 1;
                }

                aggregatedNalSize += 2 + nal.readableBytes();  // nal.length + nal.bytes
            }
            
            // 先尝试用组合包
            if (aggregatedPacketLength > 1) {
                ByteBuf[] nals = new ByteBuf[aggregatedPacketLength];
                for (int i = 0; i < aggregatedPacketLength; i++) {
                    nals[i] = (ByteBuf)nalList.remove(0);
                }

                asAggregatedPacket(nals, out);
            }
            
            // 不行用单包， 或者拆包
            while (!nalList.isEmpty()) {
                nal = (ByteBuf)nalList.remove(0);
                if (nal.readableBytes() < maxPacketSize) {
                    asSingleNALUnitPacket(nal, out);
                } else {
                    asFUAPacket(nal, out);
                }
            }
           
        } finally {
            recycle(nalList);
        }
    }
    
    private void asSingleNALUnitPacket(ByteBuf nal, List<Object> out) {
        try{
            out.add(newRTP().writeBytes(nal));
        } finally {
            nal.release();
        }
    }
    
    private void asFUAPacket(ByteBuf nal, List<Object> out) {
        try {
            byte octet = nal.readByte();
            int forbidden_zero_bit = octet & 0x80;
            int nri = octet & 0x60;
            int nal_unit_type = octet & 0x1F;
            
            
            byte fuIndicator = (byte) (0xFF & (forbidden_zero_bit | nri | 28 /* nal_unit_type FU-A */));
            byte fuHeader = (byte) (0xFF & (0x80 /* Start bit */ | 0 /* End bit */ | 0 /* Reserved bit */ | nal_unit_type));
            
            int maxFUPayloadLength = this.maxPacketSize - 2 /* FU indicator & FU header */;
            while (nal.isReadable()) {
                int fuPayloadLength;
                int nalLength = nal.readableBytes();

                if (nalLength > maxFUPayloadLength) {
                    fuPayloadLength = maxFUPayloadLength;
                } else {
                    fuPayloadLength = nalLength;
                    fuHeader |= 0x40; // Turn on the End bit.
                }

                /*
                 * Tests with Asterisk suggest that the fragments of a fragmented
                 * NAL unit must be with one and the same size. There is also a
                 * similar question on the x264-devel mailing list but,
                 * unfortunately, it is unanswered.
                 */
                // byte[] fua = new byte[ 2 /* FU indicator & FU header */ + maxFUPayloadLength];
                final ByteBuf fuaPayload = nal.readSlice(fuPayloadLength);
                MutableRtpPacket fua = newRTP();
                fua.writeByte(fuIndicator);
                fua.writeByte(fuHeader);
                fua.writeBytes(fuaPayload);
                out.add(fua);

                fuHeader &= ~0x80; // Turn off the Start bit.
            }
        } finally {
            nal.release();
        }
    }
    
    private void asAggregatedPacket(ByteBuf[] nals, List<Object> out) {
        MutableRtpPacket aggregated = newRTP();
        try {
            int nalType = 24; //  STAP-A (one packet, multiple nals)
            aggregated.writeByte(nalType);
            
            for (int i = 0; i < nals.length; i++) {
                aggregated.writeShort(nals[i].readableBytes());
                aggregated.writeBytes(nals[i]);
            }

            out.add(aggregated);
            aggregated = null; // 避免重复释放
        } finally {
            ReferenceCountUtil.release(aggregated);
            for (int i = 0; i < nals.length; i++) {
                ReferenceCountUtil.release(nals[i]);
            }
        }
    }
    
    public void setMaxPacketSize(int maxPacketSize) {
        if (maxPacketSize < 1024) {
            throw new IllegalArgumentException("NAL packet size must max then 1024");
        }

        this.maxPacketSize = maxPacketSize;
    }
}
