package com.sengled.media;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface MediaDispatcher extends Closeable {

    void dispatch(FramePacket frame);

    @Override
    void close() throws IOException;

    int getNumSinks();

    boolean addMediaSink(MediaSink newSink);

    boolean removeMediaSink(MediaSink sink);

    <T extends MediaSink> T getMediaSink(Class<T> type);

    <T extends MediaSink> List<T> getMediaSinks(Class<T> type);

    List<MediaSink> getMediaSinks();

}