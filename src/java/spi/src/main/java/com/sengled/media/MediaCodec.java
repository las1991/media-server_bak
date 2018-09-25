package com.sengled.media;

import org.mobicents.media.server.spi.MediaType;

public enum MediaCodec {
    H264(MediaType.VIDEO, 7, 97, "H264"),
    AAC(MediaType.AUDIO, 10, 96, "MPEG4-GENERIC"),
    G711(MediaType.AUDIO, 7, 8, "PCMA"),
    SPEEX(MediaType.AUDIO, 11, 97, "SPEEX"),
    UNSUPPORTED_VIDEO(MediaType.VIDEO, -1, 0, "UNSUPPORTED"),
    UNSUPPORTED_AUDIO(MediaType.AUDIO, -2, 0, "UNSUPPORTED");

    private final int codecId;
    private final MediaType type;
    private final int rtpFormat;
    private final String rtpCodecName;

    private MediaCodec(MediaType type, int codecId, int rtpFormat, String rtpCodecName) {
        this.type = type;
        this.codecId = codecId;
        this.rtpFormat = rtpFormat;
        this.rtpCodecName = rtpCodecName;
    }

    public static MediaCodec findByVideoCodeId(int codecId) {
        for (MediaCodec codec : MediaCodec.values()) {
            if (codec.isVideo() && codec.codecId == codecId) {
                return codec;
            }
        }

        return UNSUPPORTED_VIDEO;
    }

    public MediaType getMediaType() {
        return type;
    }

    public int getCodecId() {
        return codecId;
    }

    public int getRtpFormat() {
        return rtpFormat;
    }

    public String getRtpCodecName() {
        return rtpCodecName;
    }

    public boolean isAudio() {
        return MediaType.AUDIO == type;
    }

    public boolean isVideo() {
        return MediaType.VIDEO == type;
    }
}
