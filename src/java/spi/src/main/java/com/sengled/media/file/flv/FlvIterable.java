package com.sengled.media.file.flv;

import io.netty.buffer.ByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;


/**
 * Created by las on 2017/2/8.
 */
public abstract class FlvIterable implements Iterable<FLV> {

    Logger logger = LoggerFactory.getLogger(FlvIterable.class);

    protected ByteBufAllocator alloc;

    public FlvIterable(ByteBufAllocator alloc) {
        this.alloc = alloc;
    }

    public abstract int size();

    protected abstract FLV get(int i);

    public Iterator<FLV> iterator() {
        return new ItrReadOnly();
    }

    private class ItrReadOnly implements Iterator<FLV> {
        private int _index = 0;

        @Override
        public boolean hasNext() {
            return _index != size();
        }

        @Override
        public FLV next() {
            return get(_index++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


}
