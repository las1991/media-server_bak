package com.sengled.media;

import java.nio.charset.Charset;
import io.netty.buffer.Unpooled;
import junit.framework.TestCase;

public class TestMutableByteBufHolder extends TestCase {

    public void testMutable() {
        MutableByteBufHolder holder = new MutableByteBufHolder(Unpooled.compositeBuffer());
        
        System.out.println(holder.content().toString(Charset.defaultCharset()));
    }
}
