package com.sengled.media.server.rtsp.rtp.packetizer.mpeg4;

import com.sengled.media.MediaCodec;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.MutableFramePacket;
import com.sengled.media.server.rtsp.rtp.RtpPacket;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;

import java.util.List;

import static com.sengled.media.server.rtsp.rtp.packetizer.mpeg4.Mpeg4PacketConfig.MASK;

/**
 * @author chenxh
 * @see http://tools.ietf.org/html/rfc3640
 */
public class Mpeg4RtpDePacketizer extends RtpDePacketizer<AudioSpecificConfig> {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Mpeg4RtpDePacketizer.class);


    final static class AUHeaders {
        int size;
        int index;
        int cts_flag;
        int cts;
        int dts_flag;
        int dts;
        int rap_flag;
        int streamstate;

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("{AU, size=").append(size);
            buf.append(", index=").append(index);
            buf.append("}");
            return buf.toString();
        }
    }


    private final Mpeg4PacketConfig config;

    public Mpeg4RtpDePacketizer(int streamIndex, AudioSpecificConfig extra, Rational timeunit, Mpeg4PacketConfig config) {
        super(streamIndex, MediaCodec.AAC, extra, timeunit);
        this.config = config;
    }


    @Override
    protected int dePacket(StreamContext<AudioSpecificConfig> ctx, RtpPacket rtpPkt, List<Object> out) {
        ByteBuf data = rtpPkt.content();

        final int auHeader = data.readUnsignedShort();
        final int auHeaderSize = auHeader >> (16 - config.sizeLength);

        int[] sampleIndexes = new int[auHeaderSize / 2];
        int[] sampleSizes = new int[auHeaderSize / 2];
        for (int i = 0; i < sampleSizes.length; i++) {
            final int headerSection = data.readUnsignedShort();
            sampleSizes[i] = MASK[config.sizeLength] & (headerSection >> (16 - config.sizeLength));

            if (0 == i) {
                sampleIndexes[i] = MASK[config.indexLength] & (headerSection >> (16 - config.sizeLength - config.indexLength));
            } else {
                int delta = MASK[config.indexLength] & (headerSection >> (16 - config.sizeLength - config.indexDeltaLength));
                sampleIndexes[i] = 1 + sampleIndexes[i - 1] + delta;
            }
            LOGGER.trace("sampeSize={}, sampleIndex={}", sampleSizes[i], sampleIndexes[i]);
        }

        for (int index = 0; index < sampleSizes.length; index++) {
            int sampleSize = sampleSizes[index];
            int sampleIndex = sampleIndexes[index];

            ByteBuf slice = data.readSlice(sampleSize);
            MutableFramePacket frame = newFramePacket();
            frame.writeBytes(slice);
            frame.setTime(frame.getTime() + sampleIndex * 1024);
            out.add(frame);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("aac#{} pts = {}", sampleIndex, DateFormatUtils.format(frame.getTime(Rational.$1_000), "HH:mm:ss.SSS"));
            }
        }

        LOGGER.trace("left = {}, {}, {}", data.readableBytes(), config.indexLength, config.indexDeltaLength);

        return AUDIO;

    }
}
