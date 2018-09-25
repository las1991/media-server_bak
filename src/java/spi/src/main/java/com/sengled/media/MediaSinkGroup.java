package com.sengled.media;

import java.io.Closeable;
import java.util.Collection;

import com.sengled.media.server.MutableFramePacket;

public interface MediaSinkGroup extends Closeable {
    
    public int dispatch(MutableFramePacket pkt);
    
    public boolean add(MediaSink add);
    
    public boolean remove(MediaSink sink);
    
    public Collection<? extends MediaSink> getMediaSinks();

    public int size();

    public boolean isEmpty();

}
