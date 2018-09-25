package com.sengled.media.server.rtsp.rtp.packetizer.h264;

import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.codecs.h264.H264Utils;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.ReferenceCountUtil;

public class H264 {
    private static final int _2M = 2 * 1024 * 1024;

    private H264() {
    }

    public static void splitFast(ByteBuf nals, List<Object> out) {
        ByteBufAllocator alloc = nals.alloc();
        Frame frame = Memory.allocate(_2M);
        try {
            final int readableBytes = nals.readableBytes();
            nals.readBytes(frame.getData(), 0, readableBytes);
            ByteBuffer bytes = ByteBuffer.wrap(frame.getData(), 0, readableBytes);

            H264Utils.skipToNALUnit(bytes);
            int mark = bytes.position();
            while (bytes.hasRemaining()) {
                H264Utils.skipToNALUnit(bytes);

                int length = bytes.position() - mark;
                ByteBuf nal = alloc.buffer(length);
                nal.writeBytes(frame.getData(), mark, length);

                if (bytes.hasRemaining()) {
                    if (nal.readableBytes() >= 4 && nal.getInt(length - 4) == 1) {
                        nal.writerIndex(nal.writerIndex() - 4); // 实际 nal split 为 "0x00 0x00 0x00 0x01"
                    } else {
                        nal.writerIndex(nal.writerIndex() - 3); // 实际 nal split 为 "0x00 0x00 0x01"
                    }
                }


                mark = bytes.position();
                out.add(nal);
            }

        } finally {
            frame.recycle();
            nals.release();
        }
    }

    /**
     * 把以 0001 分割的 NAL 数据分割开来
     *
     * @param nals
     * @param out
     * @author chenxh
     * @see H264#splitFast(ByteBuf, List)
     */
    @Deprecated
    public static void split(ByteBuf nals, List<Object> out) {
        try {
            // 组合包访问慢
            if (nals instanceof CompositeByteBuf) {
                final CompositeByteBuf composite = (CompositeByteBuf) nals;
                nals = composite.copy();
                composite.release();
            }

            // 
            if (!skipToNALUnit(nals)) {
                throw new IllegalArgumentException("nal start code NOT found");
            }

            // split
            int readerIndex = nals.readerIndex();
            while (skipToNALUnit(nals)) {
                int nextNalStartIndex = nals.readerIndex();

                int nalSize = nextNalStartIndex - readerIndex;
                if (1 == nals.getInt(nextNalStartIndex - 4)) {
                    nalSize -= 4;
                } else if (1 == nals.getMedium(nextNalStartIndex - 3)) {
                    nalSize -= 3;
                }

                ByteBuf nal = nals.readerIndex(readerIndex).readSlice(nalSize);
                out.add(nal.retain());

                nals.readerIndex(nextNalStartIndex);
                readerIndex = nextNalStartIndex;
            }
        } finally {
            ReferenceCountUtil.release(nals);
        }
    }


    private static final boolean skipToNALUnit(ByteBuf buf) {

        if (!buf.isReadable()) {
            return false;
        }

        int val = 0xffffffff;
        while (buf.isReadable()) {
            val <<= 8;
            val |= (buf.readByte() & 0xff);
            if ((val & 0xffffff) == 1) {
                break;
            }
        }

        return true;
    }


    public static ByteBuffer makeExtra(AVCDecoderConfigurationRecord record) {
        ByteBuffer sps = record.getSps();
        ByteBuffer pps = record.getPps();
        ByteBuffer profile = record.getProfile();

        if (null == profile) {
            profile = (ByteBuffer) sps.asReadOnlyBuffer().position(1).limit(4);
        }

        ByteBuffer extra;
        int capacity = 1 + profile.remaining() + 1 + 1 + 2 + sps.remaining() + 1 + 2 + pps.remaining();
        extra = ByteBuffer.allocate(capacity);

        extra.put((byte) 0x01);
        extra.put(profile.duplicate());
        extra.put((byte) (0xFC | (H264DePacketizer.NAL_PREFIX.length - 1))); // nalUnitLength
        extra.put((byte) (0xE0 | 0x01)); // sps 个数
        extra.putShort((short) sps.remaining());
        extra.put(sps.duplicate());
        extra.put((byte) (0x01)); // pps 个数
        extra.putShort((short) pps.remaining());
        extra.put(pps.duplicate());
        extra.flip();
        return extra;
    }

}
