package com.sengled.media.server.http;

import org.apache.commons.lang.StringUtils;
import com.sengled.media.MediaSource;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.file.flv.FlvOutputEncoder;
import com.sengled.media.server.MutableFramePacket;
import com.sengled.media.server.StreamingMediaSink;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultLastHttpContent;



public class FlvStreamingMediaSink extends StreamingMediaSink {
    // private static final Logger LOGGER = LoggerFactory.getLogger(FlvStreamingMediaSink.class);


    // 保证 media-storage-v3 集群只有一个连接连到 media 上
    public static final String FROM_MEDIA_STORAGE = "from=media-storage-v3";

    private final String queryString;

    private FlvOutputEncoder encoder;

    private boolean hasWriteHeader;
    private long ptsOffset;

    public FlvStreamingMediaSink(ChannelHandlerContext output, MediaSource source, String queryString) {
        super(output, source, source.getStreamContexts());
        this.encoder = new FlvOutputEncoder(source.getStreamContexts());
        this.queryString = queryString;
    }

    @Override
    public boolean isFromStorage() {
        return StringUtils.contains(queryString, FROM_MEDIA_STORAGE);
    }


    protected void writeInterleaved(StreamContext<?> ctx, MutableFramePacket frame) {
        ByteBuf output = alloc().compositeBuffer();
        try {
            // 写文件头
            if (!hasWriteHeader) {
                hasWriteHeader = true;
                ptsOffset = frame.getTime(Rational.MILLISECONDS);
    
                // flv header
                encoder.writeFlvHeader(output);
            }
    
    
            // 交叉写音视频数据
            if (frame.getTime(Rational.MILLISECONDS) >= ptsOffset) {
                frame.setTime(frame.getTime(Rational.MILLISECONDS) - ptsOffset, Rational.MILLISECONDS);
    
                encoder.encode(frame.retain(), output);
            }

            writeAndFlush(output.retain());
        } finally {
            output.release();
        }
    }

    @Override
    protected void beforeClose0() {
        writeAndFlush(DefaultLastHttpContent.EMPTY_LAST_CONTENT);
    }
}
