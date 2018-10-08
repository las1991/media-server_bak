package com.sengled.media.server.rtsp.rtp.packetizer;


import com.sengled.media.FramePacket;
import com.sengled.media.MediaCodec;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.clock.SystemClock;
import com.sengled.media.server.MediaCodecExtra;
import com.sengled.media.server.rtsp.rtcp.RtcpPacket;
import com.sengled.media.server.rtsp.rtcp.RtcpPacketFactory;
import com.sengled.media.server.rtsp.rtp.*;
import com.sengled.media.server.rtsp.rtp.packetizer.h264.AVCDecoderConfigurationRecord;
import com.sengled.media.server.rtsp.rtp.packetizer.mpeg4.AudioSpecificConfig;
import com.sengled.media.server.rtsp.rtp.packetizer.mpeg4.Mpeg4PacketConfig;
import com.sengled.media.server.rtsp.rtp.packetizer.mpeg4.Mpeg4RtpPacketizer;
import com.sengled.media.server.rtsp.rtp.statistics.RtpStatistics;
import gov.nist.javax.sdp.fields.AttributeField;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

public abstract class RtpPacketizer<T extends MediaCodecExtra> extends RtpStreamContext<T> implements Closeable, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RtpPacketizer.class);

    private static final int SEND_RTCP_INTERVAL = 5000;

    private final RtpMockClock clock;

    private int seqNo = RandomUtils.nextInt() & 0xFFFFFF;
    private long rtpTime;

    int rtpIndex;
    private long rtcpSentOn = -1;
    protected int maxPacketSize = (1460 - 12);

    public RtpPacketizer(int streamIndex, Rational unit, StreamContext<T> src) {
        this(streamIndex, unit, src.getCodec(), src.getExtra());

        setNumChannels(src.getNumChannels());
        setSampleRate(src.getSampleRate());
        setSampleSize(src.getSampleSize());
        setWidth(src.getWidth());
        setHeight(src.getHeight());
        setFrameRate(src.getFrameRate());
    }

    public RtpPacketizer(int streamIndex, Rational timeUnit, MediaCodec codec, T extra) {
        super(streamIndex, timeUnit, codec, extra);
        this.clock = new RtpMockClock(timeUnit);
        this.setStatistics(new RtpStatistics(new RtpClock(clock), RandomUtils.nextLong(), CNAME));
    }

    public boolean readMediaDescription(MediaDescription dst) throws SdpException {
        String rtpmap = null;
        String fmtp = null;

        final MediaCodec codec = getCodec();
        switch (codec) {
            case G711:
                // a=rtpmap:8 PCMA/8000/1
                int channels = getNumChannels() > 0 ? getNumChannels() : 1;
                rtpmap = String.format("%d PCMA/%d/%d", codec.getRtpFormat(), getSampleRate(), channels);
                break;
            case SPEEX:
                // a=rtpmap:97 SPEEX/16000/1
                channels = getNumChannels() > 0 ? getNumChannels() : 1;
                rtpmap = String.format("%d %s/%d/%d", codec.getRtpFormat(), codec.getRtpCodecName(), getSampleRate(), channels);
                break;
            case AAC:
                // a=rtpmap:97 MPEG4-GENERIC/44100/2
                channels = getNumChannels() > 0 ? getNumChannels() : 2;
                rtpmap = String.format("%d MPEG4-GENERIC/%d/%d", codec.getRtpFormat(), getSampleRate(), channels);

                // a=fmtp:97 profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3; config=121056E500
                AudioSpecificConfig config = (AudioSpecificConfig) getExtra();
                if (null != config && null != config.getConfig()) {
                    Mpeg4PacketConfig mpeg4PacketConfig = ((Mpeg4RtpPacketizer) this).getConfig();
                    String format = "%s profile-level-id=%d;mode=%s;sizelength=%d;indexlength=%d;indexdeltalength=%d; config=%s";
                    String hexConfig = Hex.encodeHexString(config.getConfig());
                    fmtp = String.format(format, codec.getRtpFormat(), mpeg4PacketConfig.getProfileLevelId(), mpeg4PacketConfig.getMode(), mpeg4PacketConfig.getSizeLength(), mpeg4PacketConfig.getIndexLength(), mpeg4PacketConfig.getIndexDeltaLength(), hexConfig);
                }
                break;
            case H264:
                // a=rtpmap:96 H264/90000
                rtpmap = String.format("%d H264/90000", codec.getRtpFormat());


                // 补齐 SPS 和 PPS， 否则 aws ECHO SHOW 不能播放视频
                // a=fmtp:97 profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3; config=121056E500
                AVCDecoderConfigurationRecord record = (AVCDecoderConfigurationRecord) getExtra();
                ByteBuffer sps, pps, profile;
                sps = null != record ? record.getSps() : null;
                pps = null != record ? record.getPps() : null;
                profile = null != record ? record.getProfile() : null;
                if (null != sps && null != sps && null != profile) {

                    String format = "%s packetization-mode=1; sprop-parameter-sets=%s,%s; profile-level-id=%s";
                    String spsString = Base64.encode(Unpooled.wrappedBuffer(sps)).toString(Charset.defaultCharset());
                    String ppsString = Base64.encode(Unpooled.wrappedBuffer(pps)).toString(Charset.defaultCharset());
                    String profileString = ByteBufUtil.hexDump(Unpooled.wrappedBuffer(profile)).toUpperCase();

                    fmtp = String.format(format, codec.getRtpFormat(), spsString, ppsString, profileString);
                }
                break;
            default:
                return false;
        }

        if (null != rtpmap) {
            AttributeField f = new AttributeField();
            f.setName("rtpmap");
            f.setValue(rtpmap);
            dst.addAttribute(f);
        }

        if (null != fmtp) {
            AttributeField f = new AttributeField();
            f.setName("fmtp");
            f.setValue(fmtp);
            dst.addAttribute(f);
        }

        return true;
    }

    abstract protected void packet0(FramePacket frame, List<Object> out);

    public void packet(FramePacket frame, List<Object> out) {
        try {
            rtpIndex = 0;

            // 调整时钟，一定要先调整, 要不然后面 RTCP 计算就错了
            clock.setRtpTime(frame.getTime(), frame.getTimeUnit());
            // System.out.println(DateFormatUtils.formatUTC(clock.getCurrentTime(), "yyyy-MM-dd HH:mm:ss.SSS"));

            this.rtpTime = 0xFFFFFFFF & frame.getTime(getTimeUnit());
            final int readerIndex = out.size();

            // 
            packet0(frame, out);

            // 把第一个元素拿出来
            RtpStatistics statistics = getStatistics();
            for (int i = readerIndex; i < out.size(); i++) {
                RtpPacketI packet = (RtpPacketI) out.get(i);
                statistics.onRtpSent(packet.getPayloadLength(), packet.getTime());
            }

            // 每隔 5s 发送 rtcp 包
            if (SystemClock.currentTimeMillis() - rtcpSentOn > SEND_RTCP_INTERVAL) {
                final RtcpPacket sr = RtcpPacketFactory.buildReport(statistics);
                out.add(0, sr);

                statistics.onRtcpSent(sr);
                rtcpSentOn = SystemClock.currentTimeMillis();
            }
        } finally {
            ReferenceCountUtil.release(frame);
        }
    }


    protected MutableRtpPacket newRTP() {
        return newRTP(alloc().buffer(maxPacketSize));
    }

    private MutableRtpPacket newRTP(ByteBuf data) {
        final int profile = 0;
        final int version = 2;
        final long ssrc = getStatistics().getSsrc();

        final boolean marker;
        if (getCodec().isVideo()) {
            marker = (rtpIndex == 0); // 视频，第一个 RTP 包就是 marker
        } else {
            marker = true;
        }

        int seqNo = 0xFFFF & nextSeqNo();
        MutableRtpPacket rtp = RTP.wrap(version, marker, getPayloadType(), seqNo, rtpTime, ssrc, profile, data);

        rtpIndex++; // 标记为下一个
        return rtp;
    }

    private int nextSeqNo() {
        return this.seqNo++;
    }

    @Override
    public final void close() throws IOException {
        LOGGER.debug("{} closed", this);
        release();
    }

    @Override
    protected void finalize() throws Throwable {
        release();
    }

    public void release() throws IOException {
    }


    public long getRtpTime() {
        return rtpTime;
    }
}
