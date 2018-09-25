package org.jcodec.containers.mkv;

import static org.jcodec.common.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.MKVType.Block;
import static org.jcodec.containers.mkv.MKVType.BlockGroup;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.DocType;
import static org.jcodec.containers.mkv.MKVType.DocTypeReadVersion;
import static org.jcodec.containers.mkv.MKVType.DocTypeVersion;
import static org.jcodec.containers.mkv.MKVType.EBML;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.SimpleBlock;
import static org.jcodec.containers.mkv.MKVType.Timecode;
import static org.jcodec.containers.mkv.MKVType.findFirst;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.IOUtils;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlString;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MKVMuxerTest {

    public static File tildeExpand(String path) {
        return new File(path.replace("~", System.getProperty("user.home")));
    }

    private MKVTestSuite suite;

    
    @Test
    public void testRead10Frames() throws Exception {
        byte[][] rawFrames = new byte[10][];
        rawFrames[0] = readFileToByteArray(new File("src/test/resources/mkv/10frames01.vp8"));
        rawFrames[1] = readFileToByteArray(new File("src/test/resources/mkv/10frames02.vp8"));
        rawFrames[2] = readFileToByteArray(new File("src/test/resources/mkv/10frames03.vp8"));
        rawFrames[3] = readFileToByteArray(new File("src/test/resources/mkv/10frames04.vp8"));
        rawFrames[4] = readFileToByteArray(new File("src/test/resources/mkv/10frames05.vp8"));
        rawFrames[5] = readFileToByteArray(new File("src/test/resources/mkv/10frames06.vp8"));
        rawFrames[6] = readFileToByteArray(new File("src/test/resources/mkv/10frames07.vp8"));
        rawFrames[7] = readFileToByteArray(new File("src/test/resources/mkv/10frames08.vp8"));
        rawFrames[8] = readFileToByteArray(new File("src/test/resources/mkv/10frames09.vp8"));
        rawFrames[9] = readFileToByteArray(new File("src/test/resources/mkv/10frames10.vp8"));
        File file = new File("src/test/resources/mkv/10frames.webm");
        FileInputStream inputStream = new FileInputStream(file);
        MKVParser parser = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
        List<EbmlMaster> tree = null;
        try {
            tree = parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        EbmlMaster[] cc =  MKVType.findAll(tree, EbmlMaster.class, MKVType.Segment, MKVType.Cluster);
        inputStream = new FileInputStream(file);
        Assert.assertNotNull(cc);
        Assert.assertEquals(2, cc.length);
        
        try {    
            FileChannel frameReadingChannel = inputStream.getChannel();
            List<MkvBlock> bs = getBlocksByTrackNumber(cc[0], 1);
            Assert.assertNotNull(bs);
            Assert.assertEquals(5, bs.size());
            for (int i = 0; i < 5; i++) {
                FileChannel channel = frameReadingChannel;
                ByteBuffer bb = ByteBuffer.allocate(bs.get(i).dataLen);
                frameReadingChannel.position(bs.get(i).dataOffset);
                frameReadingChannel.read(bb);
                ByteBuffer[] frames = bs.get(i).getFrames(bb);
                
                Assert.assertNotNull(frames);
                Assert.assertEquals(1, frames.length);
                Assert.assertArrayEquals(rawFrames[i], MKVMuxerTest.bufferToArray(frames[0]));
            }
            bs = getBlocksByTrackNumber(cc[1], 1);
            Assert.assertNotNull(bs);
            Assert.assertEquals(5, bs.size());
            for (int i = 5; i < 10; i++) {
                ByteBuffer bb = ByteBuffer.allocate(bs.get(i-5).dataLen);
                frameReadingChannel.position(bs.get(i-5).dataOffset);
                frameReadingChannel.read(bb);
                ByteBuffer[] frames = bs.get(i - 5).getFrames(bb);
                Assert.assertNotNull(frames);
                Assert.assertEquals(1, frames.length);
                Assert.assertArrayEquals(rawFrames[i], MKVMuxerTest.bufferToArray(frames[0]));
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Test
    public void showKeyFrames() throws IOException {
        FileInputStream inputStream = new FileInputStream(suite.test5);
        MKVParser p = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
        List<EbmlMaster> t = null;
        try {
            t = p.parse();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        for (EbmlMaster c : MKVType.findAll(t, EbmlMaster.class, Segment, Cluster)) {
            for (EbmlBase e : c.children) {
                if (e.type.equals(SimpleBlock)) {
                    MkvBlock be = (MkvBlock) e;
                    System.out.println("offset: " + be.size() + " timecode: " + be.timecode);

                } else if (e.type.equals(BlockGroup)) {
                    MkvBlock be = (MkvBlock) MKVType.findFirst(e, BlockGroup, MKVType.Block);
                    System.out.println("offset: " + be.size() + " timecode: " + be.timecode);
                }
            }
        }
    }

    public void printTrackInfo() throws Exception {
        FileInputStream inputStream = new FileInputStream(suite.test1);
        MKVParser parser = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
        List<EbmlMaster> tree = null;
        try {
            tree = parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        EbmlUint[] tcs = MKVType.findAll(tree, EbmlUint.class, MKVType.Segment, MKVType.Cues, MKVType.CuePoint, MKVType.CueTime);
        for (EbmlUint tc : tcs)
            System.out.println("CueTime " + tc.get() + " " + tc.offset);
    }

    @Test
    public void testMatroskaBytes() throws Exception {
        Assert.assertArrayEquals(new byte[] { 0x6d, 0x61, 0x74, 0x72, 0x6f, 0x73, 0x6b, 0x61 }, "matroska".getBytes());
    }

    @Test
    public void testEBMLHeaderMuxin() throws Exception {
        EbmlMaster ebmlHeaderElem = (EbmlMaster) MKVType.createByType(EBML);

        EbmlString docTypeElem = (EbmlString) MKVType.createByType(DocType);
        docTypeElem.set("matroska");

        EbmlUint docTypeVersionElem = (EbmlUint) MKVType.createByType(DocTypeVersion);
        docTypeVersionElem.set(1);

        EbmlUint docTypeReadVersionElem = (EbmlUint) MKVType.createByType(DocTypeReadVersion);
        docTypeReadVersionElem.set(1);

        ebmlHeaderElem.add(docTypeElem);
        ebmlHeaderElem.add(docTypeVersionElem);
        ebmlHeaderElem.add(docTypeReadVersionElem);
        ByteBuffer bb = ebmlHeaderElem.getData();

        System.out.println("c: " + bb.capacity() + " p: " + bb.position() + " l: " + bb.limit());
    }

    @Test
    public void testEbmlMasterMuxig() throws Exception {
        EbmlMaster ebmlHeaderElem = (EbmlMaster) MKVType.createByType(EBML);

        EbmlString docTypeElem = (EbmlString) MKVType.createByType(DocType);
        docTypeElem.set("matroska");

        ebmlHeaderElem.add(docTypeElem);
        ByteBuffer bb = ebmlHeaderElem.getData();

        Assert.assertArrayEquals(new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, (byte) 0x8B, 0x42, (byte) 0x82, (byte) 0x88, 0x6d, 0x61, 0x74, 0x72, 0x6f, 0x73, 0x6b, 0x61 }, bb.array());
    }

    @Test
    public void testEmptyEbmlMasterMuxig() throws Exception {
        EbmlMaster ebmlHeaderElem = (EbmlMaster) MKVType.createByType(EBML);

        ByteBuffer bb = ebmlHeaderElem.getData();

        Assert.assertArrayEquals(new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, (byte) 0x80 }, bb.array());
    }

    @Before
    public void setUp() throws IOException {
        suite = MKVTestSuite.read();
        if (!suite.isSuitePresent())
            Assert.fail("MKV test suite is missing, please download from http://www.matroska.org/downloads/test_w1.html, and save to the path recorded in src/test/resources/mkv/suite.properties");
    }

    @Test
    public void copyMuxing() throws Exception {
        FileInputStream inputStream = new FileInputStream(suite.test3);
        MKVParser parser = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
        List<EbmlMaster> tree = null;
        try {
            tree = parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        
        
        
        FileInputStream remuxerInputStream = new FileInputStream(suite.test3);
        FileOutputStream os = new FileOutputStream(new File(suite.test3.getParent(), "copy-" + suite.test3.getName()));
        

        try {
            FileChannel channel = remuxerInputStream.getChannel();
            for (MkvBlock be : MKVType.findAll(tree, MkvBlock.class, Segment, Cluster, SimpleBlock)) {
                ByteBuffer bb = ByteBuffer.allocate(be.dataLen);
                
                channel.position(be.dataOffset);
                int read = channel.read(bb);
                bb.flip();
                be.readFrames(bb);
            }
            
            for (MkvBlock be : MKVType.findAll(tree, MkvBlock.class, Segment, Cluster, BlockGroup, Block)) {
                ByteBuffer bb = ByteBuffer.allocate(be.dataLen);
                
                channel.position(be.dataOffset);
                int read = channel.read(bb);
                bb.flip();
                be.readFrames(bb);
            }

            for (EbmlMaster e : tree)
                e.mux(new FileChannelWrapper(os.getChannel()));
        } finally {
            if (remuxerInputStream != null)
                remuxerInputStream.close();
            if (os != null)
                os.close();
        }

    }

    public void testName() throws Exception {
        FileInputStream inputStream = new FileInputStream(suite.test3);
        MKVParser parser = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
        List<EbmlMaster> tree = null;
        try {
            tree = parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        EbmlMaster[] cc = MKVType.findAll(tree, EbmlMaster.class, MKVType.Segment, MKVType.Cluster);
        printCueTable(cc);
    }

    private void printCueTable(EbmlMaster[] cc) {
        long time = 0;
        long predictedOffset = 0;
        for (EbmlMaster c : cc) {
            long csize = c.size();
            System.out.println("cluster " + ((EbmlUint) MKVType.findFirst(c, MKVType.Cluster, MKVType.Timecode)).get() + " size: " + csize + " predOffset: " + predictedOffset);
            long min = getMinTimecode(c, 1);
            long max = getMaxTimecode(c, 1);
            while (min <= time && time <= max) {
                System.out.println("timecode: " + time + " offset: " + c.offset);
                time += 1000;
            }
            predictedOffset += csize;
        }
    }
    
    public static long getMinTimecode(EbmlMaster c, int trackNr) {
        EbmlUint timecode = (EbmlUint) MKVType.findFirst(c, Cluster, Timecode);
        long clusterTimecode = timecode.get();
        long minTimecode = clusterTimecode;
        for (MkvBlock be : getBlocksByTrackNumber(c, trackNr))
            if (clusterTimecode + be.timecode < minTimecode)
                minTimecode = clusterTimecode + be.timecode;

        return minTimecode;
    }

    public static long getMaxTimecode(EbmlMaster c, int trackNr) {
        EbmlUint timecode = (EbmlUint) findFirst(c, Cluster, Timecode);
        long clusterTimecode = timecode.get();
        long maxTimecode = clusterTimecode;
        for (MkvBlock be : getBlocksByTrackNumber(c, trackNr))
            if (clusterTimecode + be.timecode > maxTimecode)
                maxTimecode = clusterTimecode + be.timecode;

        return maxTimecode;
    }
    
    public static List<MkvBlock> getBlocksByTrackNumber(EbmlMaster c, long nr) {
        List<MkvBlock> blocks = new ArrayList<MkvBlock>();
        for (EbmlBase child : c.children) {
            MkvBlock block = null;
            if (child.type.equals(SimpleBlock))
                block = (MkvBlock) child;
            else if (child.type.equals(BlockGroup))
                block = (MkvBlock) findFirst(child, BlockGroup, Block);
            else
                continue;

            if (block.trackNumber == nr)
                blocks.add(block);
        }
        return blocks;
    }

    @Test
    public void testBasicMathDivision() throws Exception {
        int framesPerCluster = 25;
        int i = 0;
        Assert.assertEquals(0, i%framesPerCluster);
        i=25;
        Assert.assertEquals(0, i%framesPerCluster);
        i=50;
        Assert.assertEquals(0, i%framesPerCluster);
    }
    
    @Test
    public void testBasicLinkedList() throws Exception {
        LinkedList<Integer> ll = new LinkedList<Integer>();
        ll.add(1);
        Assert.assertEquals(Integer.valueOf(1), ll.getLast());
        ll.add(2);
        Assert.assertEquals(Integer.valueOf(2), ll.getLast());
        ll.add(3);
        Assert.assertEquals(Integer.valueOf(3), ll.getLast());
    }

    public static byte[] bufferToArray(ByteBuffer bb) {
        byte[] ar = new byte[bb.remaining()];
        bb.get(ar);
        return ar;
    }

}
