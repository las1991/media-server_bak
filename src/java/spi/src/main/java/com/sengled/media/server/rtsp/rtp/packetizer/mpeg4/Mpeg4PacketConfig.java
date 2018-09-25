package com.sengled.media.server.rtsp.rtp.packetizer.mpeg4;

/**
 * MPEG-4 打包的配置
 * 
 * @author chenxh
 */
public class Mpeg4PacketConfig {
    public static final int[] MASK = new int[32];
    static {
        for (int maskIndex = 1; maskIndex < MASK.length; maskIndex++) {
            MASK[maskIndex] = (1 << maskIndex) - 1;
        }
    }
    
    public int streamtype;
    public int profileLevelId;
    public String mode;
    public int sizeLength;
    public int indexLength;
    public int indexDeltaLength;
    public int profile;

    public Mpeg4PacketConfig() {
        this(5, 15, "AAC-hbr", 13, 3, 3, 1);   
    }
    
    public Mpeg4PacketConfig(int streamtype, int profileLevelId, String mode, int sizeLength, int indexLength,
            int indexDeltaLength, int profile) {
        this.streamtype = streamtype;
        this.profileLevelId = profileLevelId;
        this.mode = mode;
        this.sizeLength = sizeLength;
        this.indexLength = indexLength;
        this.indexDeltaLength = indexDeltaLength;
        this.profile = profile;
    }

    public int getStreamtype() {
        return streamtype;
    }

    public int getProfileLevelId() {
        return profileLevelId;
    }

    public String getMode() {
        return mode;
    }

    public int getSizeLength() {
        return sizeLength;
    }

    public int getIndexLength() {
        return indexLength;
    }

    public int getIndexDeltaLength() {
        return indexDeltaLength;
    }

    public int getProfile() {
        return profile;
    }
}