package com.sengled.media.server.rtsp.rtp.packetizer.h264;

import com.sengled.media.server.NettySupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.RecyclableArrayList;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestH264 extends TestCase {
    private static NettySupport DEFAULT = new NettySupport();

    ByteBuf nals = null;

    @Override
    protected void setUp() throws Exception {
        nals = PooledByteBufAllocator.DEFAULT.heapBuffer();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 32 * 1024; i++) {
            builder.append((char) ('A' + i % 26));
        }

        nals.writeMedium(1);
        nals.writeBytes(new byte[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2});
        nals.writeInt(1);
        nals.writeBytes(new byte[]{3, 3, 3, 3, 3});
        nals.writeMedium(1);
        nals.writeBytes(builder.toString().getBytes());
    }

    @Override
    protected void tearDown() throws Exception {
        ReferenceCountUtil.release(nals);
    }

    public void testEquals() {
        List<Object> split2 = new ArrayList<>();
//        H264.splitFast(nals.duplicate().retain(), split2);

        List<Object> split = new ArrayList<>();
        H264.split(nals.duplicate().retain(), split);

        for (int i = 0; i < split.size(); i++) {
            String s1 = ByteBufUtil.hexDump((ByteBuf) split.get(i));
            String s2 = ByteBufUtil.hexDump((ByteBuf) split2.get(i));
            assertEquals(s1, s2);
        }
    }

    public void testSpeex() {
        long startAt = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 1000; j++) {
                RecyclableArrayList out = RecyclableArrayList.newInstance();
                try {
//                    H264.splitFast(nals.duplicate().retain(), out);
                } finally {
                    DEFAULT.recycle(out);
                }
            }
        }
        System.out.println("cost:" + (System.currentTimeMillis() - startAt) / 1000.0 + "s");



        startAt = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 1000; j++) {
                RecyclableArrayList out = RecyclableArrayList.newInstance();
                try {
                    H264.split(nals.duplicate().retain(), out);
                } finally {
                    DEFAULT.recycle(out);
                }
            }

        }
        System.out.println("cost:" + (System.currentTimeMillis() - startAt) / 1000.0 + "s");
    }

}
