package com.sengled.media.file.flv;

import static com.sengled.media.file.flv.FLV.Header.AUDIO_CHANNELS;
import static com.sengled.media.file.flv.FLV.Header.AUDIO_CODEC_ID;
import static com.sengled.media.file.flv.FLV.Header.AUDIO_SAMPLE_RATE;
import static com.sengled.media.file.flv.FLV.Header.AUDIO_SAMPLE_SIZE;
import static com.sengled.media.file.flv.FLV.Header.AUTHOR;
import static com.sengled.media.file.flv.FLV.Header.FRAME_RATE;
import static com.sengled.media.file.flv.FLV.Header.HEIGHT;
import static com.sengled.media.file.flv.FLV.Header.SERVER;
import static com.sengled.media.file.flv.FLV.Header.VIDEO_CODEC_ID;
import static com.sengled.media.file.flv.FLV.Header.WIDTH;
import static com.sengled.media.file.flv.FLV.TagType.AUDIO;
import static com.sengled.media.file.flv.FLV.TagType.SCRIPT;
import static com.sengled.media.file.flv.FLV.TagType.VIDEO;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.FramePacket;
import com.sengled.media.MediaCodec;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.file.flv.amf.Amf0Value;
import com.sengled.media.server.NettySupport;
import com.sengled.media.server.rtsp.rtp.packetizer.h264.AVCDecoderConfigurationRecord;
import com.sengled.media.server.rtsp.rtp.packetizer.h264.H264;
import com.sengled.media.server.rtsp.rtp.packetizer.h264.H264DePacketizer;
import com.sengled.media.server.rtsp.rtp.packetizer.mpeg4.AudioSpecificConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.RecyclableArrayList;

public class FlvOutputEncoder extends NettySupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlvOutputEncoder.class);

    private static final int FLAG_VIDEO = 1 << 0;
    private static final int FLAG_AUDIO = 1 << 2;


    private static final byte[] TAG_HEADER_VIDEO;
    private static final byte[] TAG_HEADER_AUDIO;
    private static final byte[] TAG_HEADER_METADATA;

    static {
        TAG_HEADER_VIDEO = new byte[11];
        TAG_HEADER_AUDIO = new byte[11];
        TAG_HEADER_METADATA = new byte[11];

        TAG_HEADER_VIDEO[0] = VIDEO;
        TAG_HEADER_AUDIO[0] = AUDIO;
        TAG_HEADER_METADATA[0] = SCRIPT;
    }


    private long timeStart;

    private boolean hasWriteSpecialConfigs;
    private boolean hasWriteAvcSequenceHeader;
    private StreamContext<?>[] streams;
    private Map<String, Object> props = new HashMap<>();

    public FlvOutputEncoder(StreamContext<?>[] streams) {
        this.streams = streams;

        // 回放的时候用这个属性
        setCreated(System.currentTimeMillis());
    }


    public void setCreated(long created) {
        final String createdTimeString = DateFormatUtils.formatUTC(created, FLV.Header.DATE_FORMAT);
        props.put(FLV.Header.UTC_DATE_TIME, createdTimeString);
    }


    public void writeFlvHeader(ByteBuf out) {
        // flv head
        out.writeByte('F');
        out.writeByte('L');
        out.writeByte('V');
        out.writeByte(0x01); // version
        out.writeByte(getFileFlag()); // flag
        out.writeInt(0x09);  // header size
        out.writeInt(0x00);  // first tag size


        // metadata
        ByteBuf onMetaData = out.alloc().buffer(1024);
        try {
            Amf0Value.encode(onMetaData, "onMetaData");
            Amf0Value.encode(onMetaData, getMetadata());
            encodeTag(SCRIPT, timeStart, onMetaData.retain(), out);
        } finally {
            onMetaData.release();
        }
    }


    public void encode(FramePacket frame, ByteBuf out) {
        try {
            final StreamContext<?> stream = streams[frame.getStreamIndex()];
            if (null == stream || !isStreamsReady()) {
                return;
            }

            // 写入配置信息
            if (!hasWriteSpecialConfigs) {
                hasWriteSpecialConfigs = true;
                writeSpecialConfigs(out);
            }

            // 写入音视频帧
            doEncode(stream, frame, out);
        } finally {
            frame.release();
        }
    }

    private void doEncode(StreamContext<?> context, FramePacket frame, ByteBuf out) {
        final ByteBufAllocator alloc = out.alloc();
        final MediaCodec codec = context.getCodec();

        if (codec == MediaCodec.H264 && writeAvcSequenceHeader(frame.getTime(Rational.MILLISECONDS),
                context, out)) {
            ByteBuf tagData = alloc.compositeBuffer(24);
            try {
                tagData.writeByte(frame.isKeyFrame() ? 0x17 : 0x27); // key frame + avc
                tagData.writeByte(0x01); //  AVC NALU
                tagData.writeMedium((int) 0); // Composition time offset

                readNalBytes(frame.content(), tagData); // nal tag data

                encodeTag(VIDEO, frame.getTime(Rational.MILLISECONDS), tagData.retain(), out);
            } finally {
                tagData.release();
            }
            return;
        } else if (codec == MediaCodec.AAC) {
            ByteBuf prefix = alloc.buffer();
            try {
                prefix.writeByte(0xAF); // aac, soundrate = 3
                prefix.writeByte(0x01); // aac raw

                encodeTag(AUDIO, frame.getTime(Rational.MILLISECONDS),
                        alloc.compositeBuffer()
                                .addComponent(true, prefix.retain())
                                .addComponent(true, frame.content().retain()), out);
            } finally {
                prefix.release();
            }
            return;
        } else if (codec == MediaCodec.G711) {
            ByteBuf prefix = alloc.buffer();
            try {
                // G.711(7), 8K < 11.5, snd16bit
                prefix.writeByte(0x72);

                encodeTag(AUDIO, frame.getTime(Rational.MILLISECONDS),
                        alloc.compositeBuffer()
                                .addComponent(true, prefix.retain())
                                .addComponent(true, frame.content().retain()), out);
            } finally {
                prefix.release();
            }

            return;
        } else if (codec == MediaCodec.SPEEX) {
            ByteBuf prefix = alloc.buffer();
            try {
                // SPEEX(11), 8K < 11.5, snd16bit
                //1011 0010
                prefix.writeByte(0xb2);

                encodeTag(AUDIO, frame.getTime(Rational.MILLISECONDS),
                        alloc.compositeBuffer()
                                .addComponent(true, prefix.retain())
                                .addComponent(true, frame.content().retain()), out);
            } finally {
                prefix.release();
            }

            return;
        }
    }

    private int getFileFlag() {
        int flag = 0;

        for (StreamContext<?> rtpStreamContext : streams) {
            if (null == rtpStreamContext) {
                continue;
            }

            switch (rtpStreamContext.getMediaType()) {
                case AUDIO:
                    flag |= FLAG_AUDIO;
                    break;
                case VIDEO:
                    flag |= FLAG_VIDEO;
                    break;
                default:
                    break;
            }
        }

        return flag;
    }


    private Map<String, Object> getMetadata() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.putAll(props);

        //map.put("duration", 0);
        map.put(SERVER, "SengledMediaServer 1.0");
        map.put(AUTHOR, "sengled-av-team");
        for (int i = 0; i < streams.length; i++) {
            StreamContext<?> av = streams[i];
            if (null == av) {
                continue;
            }

            switch (av.getMediaType()) {
                case AUDIO:
                    map.put(AUDIO_CODEC_ID, av.getCodec().getCodecId());
                    if (av.getSampleRate() > 0) {
                        map.put(AUDIO_SAMPLE_RATE, av.getSampleRate());
                    }
                    if (av.getNumChannels() > 0) {
                        map.put(AUDIO_CHANNELS, av.getNumChannels());
                    }
                    if (av.getSampleSize() > 0) {
                        map.put(AUDIO_SAMPLE_SIZE, av.getSampleSize());
                    }
                    //if (av.getLastPts() > 0) {
                    //    map.put(AUDIO_TIME_START, av.get);
                    //}
                    break;
                case VIDEO:
                    map.put(VIDEO_CODEC_ID, av.getCodec().getCodecId());
                    if (av.getWidth() > 0) {
                        map.put(WIDTH, av.getWidth());
                    }
                    if (av.getHeight() > 0) {
                        map.put(HEIGHT, av.getHeight());
                    }
                    // 帧率一般都是：15,23,24,25,29
                    if (av.getFrameRate() > 12 && av.getFrameRate() < 35) {
                        map.put(FRAME_RATE, av.getFrameRate());
                    }
                    //if (av.getLastPts() > 0) {
                    //    map.put(VIDEO_TIME_START, av.getLastPts());
                    //}
                    break;
                default:
                    break;
            }
        }

        return map;
    }


    private void writeSpecialConfigs(ByteBuf out) {

        // video & audio config
        for (StreamContext<?> streamContext : streams) {
            if (null == streamContext) {
                continue;
            }

            // h264
            if (streamContext.getCodec() == MediaCodec.H264) {
                writeAvcSequenceHeader(timeStart, (StreamContext<AVCDecoderConfigurationRecord>) streamContext, out);
                LOGGER.debug("write avc special configs");
                continue;
            }

            // aac
            if (streamContext.getCodec() == MediaCodec.AAC) {
                ByteBuf buf = out.alloc().buffer();
                try {
                    buf.writeByte(0xAF); // aac, soundrate = 3
                    buf.writeByte(0x00); // sequence header

                    AudioSpecificConfig config = (AudioSpecificConfig) streamContext.getExtra();
                    buf.writeBytes(config.getConfig());
                    encodeTag(AUDIO, timeStart, buf.retain(), out);
                } finally {
                    buf.release();
                }

                LOGGER.info("write aac special configs");
                continue;
            }

            // G711
            if (streamContext.getCodec() == MediaCodec.G711) {
                continue;
            }
            
            // SPEEX
            if (streamContext.getCodec() == MediaCodec.SPEEX) {
                continue;
            }
            
            
            LOGGER.warn("{} unsupported", streamContext.getCodec());
        }
    }


    private boolean isStreamsReady() {
        for (int i = 0; i < streams.length; i++) {
            StreamContext<?> stream = streams[i];
            if (null == stream) {
                continue;
            }

            switch (stream.getCodec()) {
                case H264:
                case AAC:
                    if (null == stream.getExtra() || !stream.getExtra().isReady()) {
                        return false;
                    }
                    break;
                default:
                    if (null != stream.getExtra() && !stream.getExtra().isReady()) {
                        return false;
                    }
                    break;
            }
        }

        return true;
    }


    private boolean writeAvcSequenceHeader(long pts, StreamContext<?> streamContext, ByteBuf out) {
        final AVCDecoderConfigurationRecord extra = (AVCDecoderConfigurationRecord) streamContext.getExtra();
        if (null != extra && extra.hasSPS_PPS() && !hasWriteAvcSequenceHeader) {
            ByteBuf seq = out.alloc().buffer(1024);
            try {
                seq.writeByte(0x17); // key frame + avc
                seq.writeByte(0x00); // avc sequence header
                seq.writeMedium(0x00); // Composition time offset

                AVCDecoderConfigurationRecord record = (AVCDecoderConfigurationRecord) extra;
                seq.writeBytes(record.getConfig());
                encodeTag(VIDEO, pts, seq.retain(), out);
                hasWriteAvcSequenceHeader = true;
            } finally {
                seq.release();
            }
        }

        return hasWriteAvcSequenceHeader;
    }


    private void readNalBytes(ByteBuf frame, ByteBuf out) {
        int nalSizeLength = H264DePacketizer.NAL_PREFIX.length;
        RecyclableArrayList nalList = newArrayList();
        try {
            H264.split(frame.retain(), nalList);

            for (Object object : nalList) {
                ByteBuf nal = (ByteBuf) object;
                int nalSize = nal.readableBytes();

                if (nalSizeLength == 4) {
                    out.writeInt(nalSize); // nalSizeLength = 4
                } else if (nalSizeLength == 3) {
                    out.writeMedium(nalSize); // nalSizeLength = 3
                } else {
                    throw new IllegalArgumentException("nalSizeLength is too short");
                }

                out.writeBytes(nal);
            }

        } finally {
            recycle(nalList);
        }

    }


    /**
     * @param tagType
     * @param pts
     * @param tagData will auto release after release
     * @param out
     */
    private void encodeTag(int tagType, long pts, ByteBuf tagData, ByteBuf out) {
        try {
            final int tagHeaderSize = 11;
            final int dataSize = tagData.readableBytes();

            // tag header
            // tag type
            out.writeByte(tagType);
            // data size
            out.writeMedium(dataSize);
            // timestamp
            out.writeMedium(((int) (0x00FFFFFF & pts)));
            // timestamp extended
            out.writeByte((int) (0xFF & (pts >> 24)));
            // stream id
            out.writeMedium(0);

            // tag data
            out.writeBytes(tagData);

            // pre tag size
            out.writeInt(tagHeaderSize + dataSize);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("tag[{}], pts = {}, size = {}", tagType, DateFormatUtils.formatUTC(pts, "HH:mm:ss.SSS"), dataSize);
            }
        } finally {
            tagData.release();
        }
    }


}
