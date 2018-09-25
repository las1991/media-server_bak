package com.sengled.media.dispatcher.strategy;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.FramePacket;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;
import com.sengled.media.clock.SystemClock;
import com.sengled.media.dispatcher.MediaChannel;
import io.netty.util.ReferenceCountUtil;

class MultistreamMediaDispatchStrategy extends MediaDispatchStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultistreamMediaDispatchStrategy.class);
    
    private static final int MAX_AV_TIME_DIFF = 400;
    private static final int MIN_AV_TIME_DIFF = - MAX_AV_TIME_DIFF;

    private static final long MAX_PCR_DELAY = TimeUnit.SECONDS.toMillis(5);

    private final MediaChannel[] channels;
    
    private final int pcrIndex;
    private final LinkedList<FramePacket> interleaveds = new LinkedList<>();

    private long pcrTime = -1;

    MultistreamMediaDispatchStrategy(String token, StreamContext<?>[] streams) {
        super(token);

        int pcrIndex = 0;
        for (int i = 0; i < streams.length; i++) {
            if (streams[i].getCodec().isVideo()) {
                pcrIndex = i;
            }
        }
        

        this.pcrIndex = pcrIndex;
        this.channels = new MediaChannel[streams.length];
        for (int i = 0; i < streams.length; i++) {
            this.channels[i] = new MediaChannel(token, streams[i]);
        }
    }

    @Override
    public void read(FramePacket src, List<Object> out) {
        
        try {
            final long time = src.getTime(Rational.MILLISECONDS);
            final boolean isPcrFrame = src.getStreamIndex() == pcrIndex;
            final MediaChannel channel = channels[src.getStreamIndex()];
            if (isPcrFrame) {
                // 保存 pcr 时钟
                pcrTime = time;
                
                // 从缓存中把非 pcr 帧 flush 出去
                flushInterleavedFrames(pcrTime, out);
                
                // 修正时间戳后输出
                channels[pcrIndex].feed(src.retain(), out);
                
                // 参考时钟慢了，则把所有 channel 的时钟都调整以下
                // 保证延时在 （1/2 * 最大延时） 时间内
                // 调整应该在清空所有缓存后进行，因为缓冲数据会影响到 channel 的时钟
                long delay = SystemClock.currentTimeMillis() - channels[pcrIndex].currentTimeMillis();
                if(delay > MAX_PCR_DELAY) {
                    flushInterleavedFrames(out);
                    for (int i = 0; i < channels.length; i++) {
                        channels[i].adjustToTime(SystemClock.currentTimeMillis() - MAX_PCR_DELAY / 2);
                    }
                }
            } else if (pcrTime > 0 && null != channel){
                final long delay = pcrTime - time;
                interleaveds.add(src.retain());

                // 音视频不同步在 300ms 以内，人是感觉不出来的
                if (MIN_AV_TIME_DIFF < delay && delay < MAX_AV_TIME_DIFF){
                    flushInterleavedFrames(out);
                    
                    // 当前流的时钟比参考时钟慢的，调快点
                    //  但是时钟不能往回调
                    if (channels[pcrIndex].currentTimeMillis() - channel.currentTimeMillis() > MAX_AV_TIME_DIFF) {
                        boolean adjusted = channel.adjustToTime(channels[pcrIndex].currentTimeMillis() - delay);
                        if (adjusted) {
                            LOGGER.debug("[{}] stream#{} rtp delay {}ms, channel clock delay {}ms", 
                                    token,
                                    channel.getStreamIndex(),
                                    delay,
                                    channels[pcrIndex].currentTimeMillis() - channel.currentTimeMillis()
                                    );
                        }
                    }
                } else {
                    flushIfNeed(out);
                }
                
            } else { // 还没收到 pcr 时钟，直接把数据输出
                channel.feed(src.retain(), out);
            }
            
            if (LOGGER.isDebugEnabled() && pcrIndex != src.getStreamIndex()) {
                LOGGER.debug("[{}] stream#{} delay {}ms", token, src.getStreamIndex(), channels[pcrIndex].currentTimeMillis() - channel.currentTimeMillis());
            }
        } finally {
            src.release();
        }
    }

    private void flushIfNeed(List<Object> out) {
        int num = 0;
        while (interleaveds.size() > 32) {
            FramePacket frame = interleaveds.removeFirst();
            MediaChannel channel = channels[frame.getStreamIndex()];
            
            if (null != channel) {
                channel.feed(frame, out);
            } else {
                frame.release();
            }
            
            num ++;
        }
        
        
        if (num > 0 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] flushIfNeed {} frame(s) ", token, num);
        }
    }

    private int flushInterleavedFrames(List<Object> out) {
        int num = 0;
        MediaChannel mediaChannel;
        
        while(!interleaveds.isEmpty()) {
            FramePacket first = interleaveds.peekFirst();
            mediaChannel = channels[first.getStreamIndex()];
            mediaChannel.feed(interleaveds.removeFirst(), out);
            num ++;
        }
        

        if (num > 0 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] flush {} frame(s)", token, num);
        }

        return num;
    }
    
    private int flushInterleavedFrames(long time, List<Object> out) {
        int num = 0;
        MediaChannel mediaChannel;
        
        while(!interleaveds.isEmpty()) {
            FramePacket first = interleaveds.peekFirst();
            if (time < first.getTime(Rational.MILLISECONDS)) {
                break; // 还没到时间
            }
            
            mediaChannel = channels[first.getStreamIndex()];
            mediaChannel.feed(interleaveds.removeFirst(), out);
            num ++;
        }
        
        if (num > 0 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] flush {} frame(s) before {}", token, num, time);
        }
        
        return num;
    }
    

    @Override
    public final void close() {
        // 释放内存
        release();
    }

    @Override
    protected void finalize() throws Throwable {
        release();
    }
    
    private void release() {
        while(!interleaveds.isEmpty()) {
            try{
                ReferenceCountUtil.release(interleaveds.removeFirst());
            } catch (Exception e) {
                // 释放内存，但是不做任何事情。
            }
        }
    }

}