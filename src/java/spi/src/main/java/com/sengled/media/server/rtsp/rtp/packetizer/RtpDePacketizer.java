package com.sengled.media.server.rtsp.rtp.packetizer;

import com.sengled.media.MediaCodec;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.MediaCodecExtra;
import com.sengled.media.server.MutableFramePacket;
import com.sengled.media.server.rtsp.rtcp.NtpTimeStampUtil;
import com.sengled.media.server.rtsp.rtcp.RtcpPacket;
import com.sengled.media.server.rtsp.rtcp.RtcpSenderReport;
import com.sengled.media.server.rtsp.rtp.RtpPacket;
import com.sengled.media.server.rtsp.rtp.RtpStreamContext;
import com.sengled.media.server.rtsp.rtp.statistics.RtpStatistics;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang.time.DateFormatUtils;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class RtpDePacketizer<T extends MediaCodecExtra> extends RtpStreamContext<T> implements Closeable {

    private static final int NO_VALUE = -1;

    private static final long MAX_RTP_TIME = 0xFFFFFFFFL;

    private static final Logger LOGGER = LoggerFactory.getLogger(RtpDePacketizer.class);

    public static final int UNSUPPORTED = 1 << 0;
    public static final int IMCOMPLETED = 1 << 1;
    public static final int AUDIO = 1 << 2;
    public static final int H264_START = 1 << 3;
    public static final int H264_END = 1 << 4;
    public static final int H264_SPS = 1 << 5;
    public static final int H264_PPS = 1 << 6;
    public static final int H264_SEI = 1 << 7;
    public static final int H264_KEY = 1 << 8;

    private final int defaultFrameCapacity;
    private final long maxFrameDuration;

    private long firstRtpTime = NO_VALUE;
    private long thisRtpTime = NO_VALUE;
    private long lastRtpTime = NO_VALUE;

    private long srNtpTime = NO_VALUE;
    private long srRtpTime = NO_VALUE;
    private int srLoop = 0;

    /**
     * Keeps track of last (input) sequence number in order to avoid
     * inconsistent data.
     */
    private long lastSequenceNumber = -1;

    /**
     * 读取 16 进制的字符串
     *
     * @param value
     * @return
     */
    public static ByteBuffer readHex(String value) {
        byte[] array = Hex.decode(value);

        return ByteBuffer.wrap(array);
    }

    /**
     * 读取 base64 的字符串
     *
     * @param value
     * @return
     */
    public static ByteBuffer readBase64(String value) {
        byte[] array = Base64.decode(value);

        return ByteBuffer.wrap(array);
    }

    public RtpDePacketizer(int streamIndex, MediaCodec codec, T extra, Rational timeUnit) {
        super(streamIndex, timeUnit, codec, extra);

        // rtp 解包时，音频默认空间为 0， 视频默认空间为 2k
        this.defaultFrameCapacity = codec.isVideo() ? 2048 : 0;
        this.maxFrameDuration = timeUnit.convert(7000, Rational.MILLISECONDS);
    }

    /**
     * 数据包解压缩
     *
     * @param rtpPkt
     * @param out
     * @return
     */
    final public int dePacket(RtpPacket rtpPkt, List<Object> out) {
        if (null == rtpPkt) {
            return 0;
        }

        try {
            thisRtpTime = rtpPkt.time();
            if (firstRtpTime < 0) {
                firstRtpTime = thisRtpTime;
            }

            // rtp 越界了
            if (lastRtpTime + maxFrameDuration > MAX_RTP_TIME && thisRtpTime < maxFrameDuration) {
                srLoop++;

                long now = Rational.MILLISECONDS.convert(currentTime(), getTimeUnit());
                LOGGER.warn("loop = {}, now = {}, {} -> {}",
                        srLoop,
                        DateFormatUtils.format(now, "yyyy-MM-dd HH:mm:ss.SSS"),
                        lastRtpTime,
                        thisRtpTime);
            }

            getStatistics().onRtpReceive(rtpPkt);

            if (LOGGER.isTraceEnabled()) {
                long sequenceNumber = rtpPkt.seqNumber();
                long exceptSequenceNumber = 0xFFFF & (lastSequenceNumber + 1);
                if (sequenceNumber < exceptSequenceNumber) {
                    LOGGER.trace("except seqNumber is {}, but real is {}, last is {}", exceptSequenceNumber, sequenceNumber, lastSequenceNumber);
                }
                lastSequenceNumber = sequenceNumber;
            }

            return dePacket(this, rtpPkt, out);
        } finally {
            lastRtpTime = thisRtpTime;
            rtpPkt.release();
        }
    }

    public void dePacket(RtcpPacket rtcpPacket) {
        final RtpStatistics statistics = getStatistics();
        statistics.onRtcpReceive(rtcpPacket);

        RtcpSenderReport sr = rtcpPacket.getSenderReport();
        if (null != sr) {
            final long millis = NtpTimeStampUtil.getTime(sr.getNtpSec(), sr.getNtpFrac());
            srNtpTime = getTimeUnit().convert(millis, Rational.$1_000);
            srRtpTime = sr.getRtpTs();
            srLoop = 0;

            // 
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("stream#{}, t= {}, {}",
                        getStreamIndex(), DateFormatUtils.format(millis, "yyyy-MM-dd HH:mm:ss.SSS"),
                        rtcpPacket);
            }
        }
    }

    protected MutableFramePacket newFramePacket() {
        final boolean video = getCodec().isVideo();
        final long ntpTs = currentTime();

        final ByteBuf buffer = video ? alloc().compositeBuffer(64) : alloc().buffer(defaultFrameCapacity);
        final long duration = MAX_RTP_TIME & (thisRtpTime - lastRtpTime);
        MutableFramePacket frame = new MutableFramePacket(streamIndex, timeUnit, buffer);
        frame.setDuration(duration);
        frame.setTime(ntpTs);
        LOGGER.trace("{}", frame);
        return frame;
    }

    public long currentTime() {
        long ntpTs = 0;

        if (srNtpTime > 0) {
            ntpTs = srNtpTime + (srLoop * MAX_RTP_TIME) + (thisRtpTime - srRtpTime);
        } else if (firstRtpTime > 0) {
            ntpTs = (srLoop * MAX_RTP_TIME) + (thisRtpTime - firstRtpTime);
        } else {
            ntpTs = 0;
        }
        return ntpTs;
    }


    public final void close() {
        release();
        LOGGER.debug("{} closed", this);
    }

    @Override
    protected void finalize() throws Throwable {
        release();
    }

    public void release() {
    }

    protected abstract int dePacket(StreamContext<T> ctx, RtpPacket rtpPacket, List<Object> out);
}
