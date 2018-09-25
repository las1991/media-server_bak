package com.sengled.media.file.segment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;
import com.google.common.io.Files;
import junit.framework.TestCase;

public class TestFileMove extends TestCase {

    public void testMove() throws IOException {
        File src = new File("/dev/shm/mediav3", "a");
        File dst = new File("/dev/shm/storage/flv", "b");
        
        src.getParentFile().mkdirs();
        dst.getParentFile().mkdirs();
        
        Files.write("Hello World", src, Charset.defaultCharset());
        
        assertTrue(src.renameTo(dst));
    }
}
