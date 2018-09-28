/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sengled.media.server.rtsp.rtp.packetizer.h264;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.FramePacket.Flags;
import com.sengled.media.MediaCodec;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.server.MutableFramePacket;
import com.sengled.media.server.rtsp.rtp.RtpPacketI;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;


/**
 * Implements <tt>Codec</tt> to represent a depacketizer of H.264 RTP packets
 * into NAL units.
 *
 * @author Lyubomir Marinov
 * @author Damian Minkov
 */
public class H264DePacketizer extends RtpDePacketizer<AVCDecoderConfigurationRecord> {

    private static final int MAX_FRAME_PACKET_SIZE = 1 * 1024 * 1024;

    /**
     * The <tt>Logger</tt> used by the <tt>DePacketizer</tt> class and its
     * instances for logging output.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(H264DePacketizer.class);

    /**
     * The bytes to prefix any NAL unit to be output by this
     * <tt>DePacketizer</tt> and given to a H.264 decoder. Includes
     * start_code_prefix_one_3bytes. According to "B.1 Byte stream NAL unit
     * syntax and semantics" of "ITU-T Rec. H.264 Advanced video coding for
     * generic audiovisual services", zero_byte "shall be present" when "the
     * nal_unit_type within the nal_unit() is equal to 7 (sequence parameter
     * set) or 8 (picture parameter set)" or "the byte stream NAL unit syntax
     * structure contains the first NAL unit of an access unit in decoding
     * order".
     */
    public static final byte[] NAL_PREFIX = {0, 0, 0, 1};

    /**
     * Constants used to detect H264 keyframes in rtp packet
     */
    public static final byte kTypeMask = 0x1F;
    // Nalu
    public static final byte kIdr = 5;
    public static final byte kSei = 6;
    public static final byte kSps = 7;
    public static final byte kPps = 8;
    public static final byte kStapA = 24;
    public static final byte kFuA = 28;

    // Header sizes
    private static final int kNalHeaderSize = 1;
    private static final int kFuAHeaderSize = 2;
    private static final int kLengthFieldSize = 2;
    private static final int kStapAHeaderSize = kNalHeaderSize + kLengthFieldSize;
    private static final int kNalUSize = 2;


    /**
     * The Unspecified <tt>nal_unit_type</tt> as defined by the ITU-T
     * Recommendation for H.264.
     */
    private static final int UNSPECIFIED_NAL_UNIT_TYPE = 0;


    /**
     * Keeps track of last (input) sequence number in order to avoid
     * inconsistent data.
     */
    private long lastSequenceNumber = -1;

    /**
     * The <tt>nal_unit_type</tt> as defined by the ITU-T Recommendation for
     * H.264 of the last NAL unit given to this <tt>DePacketizer</tt> for
     * processing. In the case of processing a fragmentation unit, the value is
     * equal to the <tt>nal_unit_type</tt> of the fragmented NAL unit.
     */
    private int nal_unit_type = UNSPECIFIED_NAL_UNIT_TYPE;

    /**
     * The indicator which determines whether this <tt>DePacketizer</tt> is to
     * request a key frame from the remote peer associated with
     * {@link #keyFrameControl}.
     */
    private boolean requestKeyFrame = false;


    /**
     * FU-A 组合包， 当遇到一个 start_bit 后有效
     */
    private MutableFramePacket fuaFramePacket;

    private ByteBuffer sps;
    private ByteBuffer pps;
    private ByteBuffer sei;

    private int packetizationMode;

    /**
     * Initializes a new <tt>DePacketizer</tt> instance which is to depacketize
     * H.264 RTP packets into NAL units.
     *
     * @param ctx
     * @param alloc
     */
    public H264DePacketizer(int streamIndex, AVCDecoderConfigurationRecord extra, Rational timeunit) {
        super(streamIndex, MediaCodec.H264, extra, timeunit);
    }

    /**
     * Extracts a fragment of a NAL unit from a specific FU-A RTP packet
     * payload.
     *
     * @param in             the payload of the RTP packet from which a FU-A fragment of a
     *                       NAL unit is to be extracted
     * @param inOffset       the offset in <tt>in</tt> at which the payload begins
     * @param inLength       the length of the payload in <tt>in</tt> beginning at
     *                       <tt>inOffset</tt>
     * @param fuaFramePacket the <tt>Buffer</tt> which is to receive the extracted
     *                       FU-A fragment of a NAL unit
     * @return the flags such as <tt>BUFFER_PROCESSED_OK</tt> and
     * <tt>OUTPUT_BUFFER_NOT_FILLED</tt> to be returned by
     * {@link #process(Buffer, Buffer)}
     */
    private int dePacketizeFUA(List<Object> out, ByteBuf in, int profile, RtpPacketI rtp) {
        int ret = 0;
        byte fu_indicator = in.readByte();
        byte fu_header = in.readByte();
        int nal_unit_type = fu_header & 0x1F;

        this.nal_unit_type = nal_unit_type;

        boolean start_bit = (fu_header & 0x80) != 0;
        boolean end_bit = (fu_header & 0x40) != 0;

        if (start_bit) {
            LOGGER.trace("FUA nal:{},time:{} start", nal_unit_type, rtp.getTime());
            ret |= H264_START;
            /*
             * 把之前收到的 RTP 数据整合成一个不完整的数据包 flush 出去
             */
            flushImcompleteFrame(out);

            /*
             * The Start bit and End bit MUST NOT both be set in the same FU
             * header.
             */
            if (end_bit) {
                return UNSUPPORTED;
            }

            fuaFramePacket = newFramePacket();
            addFrameFlags(fuaFramePacket, profile);

            if (nal_unit_type == kIdr) {
                ret |= H264_KEY;
                fuaFramePacket.addFlag(Flags.KEY_FRAME);

                setKeyFrameExtra(fuaFramePacket);
            }

            int octet = (fu_indicator & 0xE0) /* forbidden_zero_bit & NRI */
                    | nal_unit_type;

            // Copy in the NAL start sequence and the (reconstructed) octet.
            fuaFramePacket.writeBytes(NAL_PREFIX);
            fuaFramePacket.writeByte(octet & 0xFF);
        }


        if (null == fuaFramePacket) {
            return UNSUPPORTED;
        }


        addFrameFlags(fuaFramePacket, profile);
        fuaFramePacket.writeBytes(in);
        if (end_bit) {
            LOGGER.trace("FUA nal:{},time:{} end", nal_unit_type, rtp.getTime());
            ret |= H264_END;
            flushFrame(out);
            fuaFramePacket = null;
        }

        // 每个 H264帧都应该有最大的数据包
        else if (null != fuaFramePacket && fuaFramePacket.content().readableBytes() > MAX_FRAME_PACKET_SIZE) {
            flushImcompleteFrame(out);
            LOGGER.error("frame size {} is too large, flushed", fuaFramePacket.content().readableBytes());
        }

        return ret;
    }

    private void setKeyFrameExtra(MutableFramePacket framePacket) {
        if (null == sps || null == pps) {
            return;
        }

        final boolean isEmptyFrame = !framePacket.content().isReadable();
        final boolean isKeyFrame = framePacket.isKeyFrame();
        if (isKeyFrame && isEmptyFrame) {
            AVCDecoderConfigurationRecord record = getExtra();

            boolean hasSPS_PPS = record.hasSPS_PPS();
            if (!sps.equals(record.getSps()) || !pps.equals(record.getPps())) {
                record.setSPS_PPS(Unpooled.wrappedBuffer(sps).copy().nioBuffer(), Unpooled.wrappedBuffer(pps).copy().nioBuffer());

                if (hasSPS_PPS) {
                    framePacket.addFlag(Flags.DISCONTINUE);
                    LOGGER.info("stream discontinued");
                }
            }

            framePacket.writeBytes(NAL_PREFIX);
            framePacket.writeBytes(sps.asReadOnlyBuffer());
            framePacket.writeBytes(NAL_PREFIX);
            framePacket.writeBytes(pps.asReadOnlyBuffer());
            if (null != sei) {
                framePacket.writeBytes(NAL_PREFIX);
                framePacket.writeBytes(sei.asReadOnlyBuffer());
            }
        }
    }


    private ByteBuffer copy(ByteBuf in, ByteBuffer out) {
        int readableBytes = in.readableBytes();
        LOGGER.trace("nal:{}, {} bytes", nal_unit_type, readableBytes);

        if (null == out) {
            out = ByteBuffer.allocate(readableBytes);
        } else if (out.clear().remaining() != readableBytes) {
            out = ByteBuffer.allocate(readableBytes);
            LOGGER.warn("{} discontinued, data size = {}", this, readableBytes);
        }

        // out.remaining() 的长度必须等于 in.readableBytes();
        // 否则 in.readBytes(out) 会报错
        assert out.remaining() == in.readableBytes();

        in.readBytes(out);
        out.flip();

        return out;
    }


    @Override
    protected int dePacket(StreamContext<AVCDecoderConfigurationRecord> ctx, RtpPacketI inBuffer, List<Object> out) {
        /*
         * We'll only be depacketizing, we'll not act as an H.264 parser.
         * Consequently, we'll only care about the rules of
         * packetizing/depacketizing. For example, we'll have to make sure that
         * no packets are lost and no other packets are received when
         * depacketizing FU-A Fragmentation Units (FUs).
         */
        long sequenceNumber = inBuffer.getSeqNumber();
        int ret;

        /*
         * Ignore the RTP time stamp reported by JMF because it is not the
         * actual RTP packet time stamp send by the remote peer but some locally
         * calculated JMF value.
         */
        long exceptSequenceNumber = 0xFFFF & (lastSequenceNumber + 1);
        if (sequenceNumber < exceptSequenceNumber && LOGGER.isTraceEnabled()) {
            LOGGER.trace("except seqNumber is {}, but real is {}, last is {}", exceptSequenceNumber, sequenceNumber, lastSequenceNumber);
        }

        lastSequenceNumber = sequenceNumber;

        /*
         * get first byte of RTP.
         */
        int profile = inBuffer.getFlags();
        ByteBuf payload = inBuffer.content();
        byte octet = payload.getByte(payload.readerIndex());

        /*
         * NRI equal to the binary value 00 indicates that the content of the
         * NAL unit is not used to reconstruct reference pictures for inter
         * picture prediction. Such NAL units can be discarded without risking
         * the integrity of the reference pictures. However, it is not the place
         * of the DePacketizer to take the decision to discard them but of the
         * H.264 decoder.
         */

        /*
         * The nal_unit_type of the NAL unit given to this DePacketizer for
         * processing. In the case of processing a fragmentation unit, the value
         * is equal to the nal_unit_type of the fragmentation unit, not the
         * fragmented NAL unit and is thus in contrast with the value of the
         * nal_unit_type field of this DePacketizer.
         */
        int nal_unit_type = octet & 0x1F;

        // Single NAL Unit Packet
        if ((nal_unit_type >= 0) && (nal_unit_type <= 23)) {

            ret = dePacketizeSingleNALUnitPacket(
                    out,
                    payload,
                    profile,
                    nal_unit_type,
                    inBuffer);

        } else if (nal_unit_type == 28) { // FU-A Fragmentation unit (FU)
            ret = dePacketizeFUA(out, payload, profile, inBuffer);
        } else if (nal_unit_type == 24) { // STAP-A (one packet, multiple nals)
            ret = dePacketizeAggregated(out, payload, profile, inBuffer);
        } else {
            flushImcompleteFrame(out);

            LOGGER.warn("Dropping NAL unit of unsupported type {}, {} bytes", nal_unit_type, payload.readableBytes());
            payload.readSlice(payload.readableBytes()); // skip nals

            this.nal_unit_type = nal_unit_type;

            ret = IMCOMPLETED;
        }

        return ret;
    }

    /**
     * Extract a single (complete) NAL unit from RTP payload.
     *
     * @param out           the <tt>Buffer</tt> which is to receive the extracted
     *                      NAL unit
     * @param nal_unit_type unit type of NAL
     * @param in            the payload of the RTP packet
     * @param inOffset      the offset in <tt>in</tt> at which the payload begins
     * @param inLength      the length of the payload in <tt>in</tt> beginning at
     *                      <tt>inOffset</tt>
     * @return the flags such as <tt>BUFFER_PROCESSED_OK</tt> and
     * <tt>OUTPUT_BUFFER_NOT_FILLED</tt> to be returned by
     * {@link #process(Buffer, Buffer)}
     */
    private int dePacketizeSingleNALUnitPacket(List<Object> out, ByteBuf rtp, int profile,
                                               int nal_unit_type, RtpPacketI inBuffer) {
        int ret = H264_START | H264_END; // frame start & end

        this.nal_unit_type = nal_unit_type;

        switch (nal_unit_type) {
            case kSei:
                sei = copy(rtp, sei);
                ret |= H264_SEI;
                break;
            case kSps:
                sps = copy(rtp, sps);
                ret |= H264_SPS;
                break;
            case kPps:
                pps = copy(rtp, pps);
                ret |= H264_PPS;
                break;
            default:
                MutableFramePacket frame = newFramePacket();
                addFrameFlags(frame, profile);
                try {

                    if (nal_unit_type == kIdr) {
                        ret |= (H264_KEY);
                        frame.addFlag(Flags.KEY_FRAME);
                        setKeyFrameExtra(frame);
                    }
                    frame.writeBytes(NAL_PREFIX);
                    frame.writeBytes(rtp);
                    out.add(frame.retain());
                } finally {
                    frame.release();
                }
                break;
        }

        return ret;
    }

    /**
     * 考虑到 2.2.119 之前 profile 为  0xbede ( 1011, 1110, 1101, 1110) 为既成事实，为了实现对老版本的兼容
     * 我们确定代码为：
     * 1、(profile & 0x8000) > 0  表示： 有 MD
     * 2、(profile & 0x0001) > 0  表示： 支持 PIR
     * 3、(profile & 0x0020) > 0  表示： 有 PIR 检测结果
     *
     * @param frame
     * @param profile
     */
    private void addFrameFlags(MutableFramePacket frame, int profile) {
        if (profile > 0) {
            frame.addFlag((profile & 0x8000) > 0 ? Flags.MD_FRAME : 0);
            frame.addFlag((profile & 0x0020) > 0 ? Flags.PIR_FRAME : 0);
            frame.addFlag((profile & 0x0001) > 0 ? Flags.HAS_PIR : 0);

            // 调试专用
            if (profile != 0xbede && LOGGER.isTraceEnabled()) {
                LOGGER.trace("profile= {} ", profile);
            }
        }

    }

    private int dePacketizeAggregated(List<Object> out, ByteBuf payload, int profile, RtpPacketI inBuffer) {
        int ret = H264_START | H264_END;
        MutableFramePacket frame = newFramePacket();
        try {
            addFrameFlags(frame, profile);

            // skip nal type
            payload.readByte();

            while (payload.readableBytes() > 2) {
                int nalSize = payload.readUnsignedShort();
                if (nalSize > payload.readableBytes() || nalSize < 1) {
                    LOGGER.error("illegal nal size {} while only {} readable bytes", nalSize, payload.readableBytes());
                    break;
                }
                ByteBuf nalUnit = payload.readSlice(nalSize);
                nal_unit_type = nalUnit.getByte(nalUnit.readerIndex()) & 0x1F;
                LOGGER.debug("nal = {}, len = {}", nal_unit_type, nalUnit.readableBytes());

                switch (nal_unit_type) {
                    case kSei:
                        sei = copy(nalUnit, sei);
                        ret |= H264_SEI;
                        break;
                    case kSps:
                        sps = copy(nalUnit, sps);
                        ret |= H264_SPS;
                        break;
                    case kPps:
                        pps = copy(nalUnit, pps);
                        ret |= H264_PPS;
                        break;
                    case kIdr:
                        ret |= H264_KEY;
                        frame.addFlag(Flags.KEY_FRAME);
                        setKeyFrameExtra(frame);
                    default:
                        frame.writeBytes(NAL_PREFIX);
                        frame.writeBytes(nalUnit);
                        break;
                }
            }

            if (frame.content().isReadable()) {
                out.add(frame.retain());
            }
        } finally {
            frame.release();
        }
        return ret;
    }

    @Override
    public void release() {
        if (null != fuaFramePacket) {
            LOGGER.debug("release {} bytes", fuaFramePacket.content().readableBytes());
            ReferenceCountUtil.release(fuaFramePacket);
            fuaFramePacket = null;
        }
    }

    private void flushImcompleteFrame(List<Object> out) {
        if (null != fuaFramePacket) {
            fuaFramePacket.addFlag(Flags.IMCOMPLETE_FRAME);
            flushFrame(out);
        }
    }

    private void flushFrame(List<Object> out) {
        if (null == fuaFramePacket) {
            return;
        }

        try {
            // Should we request a key frame.
            switch (this.nal_unit_type) {
                case kIdr /* Coded slice of an IDR picture */:

                    /*
                     * While it seems natural to not request a key frame in the presence of
                     * 5, 7 and 8 often seem to be followed by 5 so do not request a key
                     * frame if either 7 or 8 is present.
                     */
                case kSps /* Sequence parameter set */:
                case kPps /* Picture parameter set */:
                    requestKeyFrame = false;
                    break;
                default:
                    break;
            }

            // 需要等待关键帧
            if (requestKeyFrame && !fuaFramePacket.isKeyFrame()) {
                return;
            }

            out.add(fuaFramePacket.retain());
        } finally {
            fuaFramePacket.release();
            fuaFramePacket = null;
        }
    }


    /**
     * Returns true if the buffer contains a H264 key frame at offset
     * <tt>offset</tt>.
     *
     * @param buff the byte buffer to check
     * @param off  the offset in the byte buffer where the actuall data starts
     * @param len  the length of the data in the byte buffer
     * @return true if the buffer contains a H264 key frame at offset
     * <tt>offset</tt>.
     */
    public static boolean isKeyFrame(byte[] buff, int off, int len) {
        if (buff == null || buff.length < off + Math.max(len, 1)) {
            return false;
        }

        int nalType = buff[off] & kTypeMask;
        // Single NAL Unit Packet
        if (nalType == kFuA) {
            // Fragmented NAL units (FU-A).
            if (parseFuaNaluForKeyFrame(buff, off, len)) {
                return true;
            }
        } else {
            if (parseSingleNaluForKeyFrame(buff, off, len)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a a fragment of a NAL unit from a specific FU-A RTP packet
     * payload is keyframe or not
     */
    private static boolean parseFuaNaluForKeyFrame(byte[] buff, int off, int len) {
        if (len < kFuAHeaderSize) {
            return false;
        }
        return ((buff[off + 1] & kTypeMask) == kIdr);
    }

    /**
     * Checks if a a fragment of a NAL unit from a specific FU-A RTP packet
     * payload is keyframe or not
     */
    private static boolean parseSingleNaluForKeyFrame(byte[] buff, int off, int len) {
        int naluStart = off + kNalHeaderSize;
        int naluLength = len - kNalHeaderSize;
        int nalType = buff[off] & kTypeMask;
        if (nalType == kStapA) {
            // Skip the StapA header (StapA nal type + length).
            if (len <= kStapAHeaderSize) {
                LOGGER.error("StapA header truncated.");
                return false;
            }
            if (!verifyStapANaluLengths(buff, naluStart, naluLength)) {
                LOGGER.error("StapA packet with incorrect NALU packet lengths.");
                return false;
            }
            nalType = buff[off + kStapAHeaderSize] & kTypeMask;
        }
        return (nalType == kIdr || nalType == kSps ||
                nalType == kPps || nalType == kSei);
    }

    private static boolean verifyStapANaluLengths(byte[] data, int offset,
                                                  int lengthRemaining) {
        int initialLength = lengthRemaining;
        while (lengthRemaining > 0 && offset + 1 < initialLength && offset > 0) {
            // Buffer doesn't contain room for additional nalu length.
            if (lengthRemaining < kNalUSize) {
                return false;
            }
            int naluSize = data[offset] << 8 | data[offset + 1];
            offset += kNalUSize;
            lengthRemaining -= kNalUSize;
            if (naluSize > lengthRemaining) {
                return false;
            }
            offset += naluSize;
            lengthRemaining -= naluSize;
        }
        return true;
    }

    public void setPacketizationMode(int packetizationMode) {
        this.packetizationMode = packetizationMode;
    }
}