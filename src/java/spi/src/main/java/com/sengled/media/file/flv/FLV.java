package com.sengled.media.file.flv;

import io.netty.buffer.ByteBuf;

public final class FLV {
    private FLV() {
    }

    public static final int[] SoundRate = new int[]{5500, 11000, 22000, 44000};

    public static class TagType {
        public static final int AUDIO = 8;
        public static final int VIDEO = 9;
        public static final int SCRIPT = 18;
    }

    public static class Header {
        public static final String UTC_DATE_TIME = "utcDateTime";
        public static final String AUTHOR = "builder";
        public static final String SERVER = "server";
        // public static final String VIDEO_TIME_START = "videotimestart";
        public static final String FRAME_RATE = "framerate";
        public static final String HEIGHT = "height";
        public static final String WIDTH = "width";
        public static final String VIDEO_CODEC_ID = "videocodecid";
        // public static final String AUDIO_TIME_START = "audiotimestart";
        public static final String AUDIO_SAMPLE_SIZE = "audiosamplesize";
        public static final String AUDIO_CHANNELS = "audiochannels";
        public static final String AUDIO_SAMPLE_RATE = "audiosamplerate";
        public static final String AUDIO_CODEC_ID = "audiocodecid";
        
        // 这个格式不要动， 存储服务需要的
        public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    }

    private ByteBuf header;

    private ByteBuf body;

    public FLV(ByteBuf flv) throws Exception {
        if ('F' != flv.readByte() || 'L' != flv.readByte() || 'V' != flv.readByte()) {
            throw new RuntimeException("file is not flv");
        }
        flv.resetReaderIndex();
        header = flv.readBytes(9);
        int firstTagSize = flv.readInt();//0
        body = flv.copy();
    }

    public boolean hasAudio() {
        return (header.getByte(5) & 0x04) > 0;
    }

    public boolean hasVideo() {
        return (header.getByte(5) & 0x01) > 0;
    }

    public FlvTag getNextTag() {
        if (!body.isReadable()) return null;
        int size = body.getMedium(body.readerIndex() + 1) + 11;
        ByteBuf tag = body.readBytes(size);
        int tagSize = body.readInt();
        return new FlvTag(tag);
    }

    public ByteBuf getHeader() {
        return header;
    }

    public boolean hasNextTag() {
        return body.isReadable();
    }

    public void release() {
        header.release();
        body.release();
    }
}
