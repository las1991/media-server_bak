package com.sengled.media.file.flv;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

/**
 * Created by las on 2017/2/8.
 */
public class FlvTag extends DefaultByteBufHolder {
    
    public FlvTag(ByteBuf data) {
        super(data);
    }

    public int getTagType() {
        return content().getByte(0);
    }

    public int getDataSize() {
        return content().getMedium(1);
    }

    public int getTimestamp() {
        return content().getMedium(4);
    }

    public int getTimestampExtended() {
        return content().getByte(7);
    }

    public int getStreamID() {
        return content().getMedium(8);
    }

    public ByteBuf getData() {
        return content().copy(11, content().readableBytes() - 11);
    }
}
