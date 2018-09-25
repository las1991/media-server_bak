package com.sengled.media.server;

import org.mobicents.media.server.spi.MediaType;
import com.sengled.media.MediaCodec;
import com.sengled.media.StreamContext;
import com.sengled.media.clock.Rational;

public class DefaultStreamContext<T extends MediaCodecExtra> extends NettySupport implements StreamContext<T> {

    protected final int streamIndex;
    protected final Rational timeUnit;

    private MediaCodec codec;
    private T extra;
    private int width = 0;
    private int height = 0;
    private double frameRate;
    private int sampleRate;
    private int sampleSize;
    private int numChannels;

    public DefaultStreamContext(int streamIndex, MediaCodec codec, T extra, Rational timeUnit) {
        this.timeUnit = timeUnit;
        this.codec = codec;
        this.streamIndex = streamIndex;
        this.extra = extra;
    }


    public void setCodec(MediaCodec codec) {
        this.codec = codec;
    }

    public T getExtra() {
        return extra;
    }

    public int getStreamIndex() {
        return streamIndex;
    }


    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }
    
    
    @Override
    public Rational getTimeUnit() {
        return timeUnit;
    }

    public MediaType getMediaType() {
        return codec.getMediaType();
    }

    public MediaCodec getCodec() {
        return codec;
    }

    public int getWidth() {
        return width;
    }

    
    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public double getFrameRate() {
        return frameRate;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    @Override
    public int getNumChannels() {
        return numChannels;
    }

    public void setFrameRate(double frameRate) {
        this.frameRate = frameRate;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setNumChannels(int numChannels) {
        this.numChannels = numChannels;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setWidth(int width) {
        this.width = width;
    }

}