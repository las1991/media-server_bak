package org.jcodec.codecs.h264.conformance;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.MappedH264ES;
import org.jcodec.common.IOUtils;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;

/**
 * A tool to test jcodec for conformance
 * 
 * @author Jay Codec
 * 
 */
public class ConformanceTestTool {
    public static void main(String[] args) throws IOException {

        (new ConformanceTestTool()).doIt(args);
    }

    private void doIt(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Syntax: conformace <directory> <report.html>");
            return;
        }

        File dir = new File(args[0]);

        StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        int i = 0;
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                processOneDir(file, sb, i);
                i++;
            }
        }

        sb.append("</table>");

        System.out.println("TESTING DONE");
        NIOUtils.writeTo(ByteBuffer.wrap(sb.toString().getBytes()), new File(args[1]));
    }

    private void processOneDir(File file, StringBuilder sb, int i) throws IOException {

        File[] listFiles = file.listFiles();
        File coded = null, decoded = null;

        // Check the candidate
        for (File file2 : listFiles) {
            if (file2.isDirectory())
                return;
            String name = file2.getName();
            if (name.endsWith(".264") || name.endsWith(".h264")) {
                coded = file2;
            } else if (name.endsWith(".yuv") || name.endsWith(".raw")) {
                decoded = file2;
            }
        }

        if (coded == null || decoded == null) {
            System.out.println("No suitable files found for directory: " + file.getName());
            return;
        }

        sb.append("<tr><td>" + (i + 1) + "</td><td><a href=\"http://storage.jcodec.org/h264/baseline/" + file.getName()
                + ".zip\">" + file.getName() + ".zip</a></td><td>");

        System.out.print("-- " + file.getName());

        doOneTest(coded, decoded, sb);

        sb.append("</td><td>&nbsp;</td></tr>");
    }

    private void doOneTest(File coded, File decoded, StringBuilder sb) {
        try {
            RawReader rawReader = null;
            int oldWidth = 0, oldHeight = 0;
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(coded));
                MappedH264ES demuxer = new MappedH264ES(NIOUtils.map(coded));
                H264Decoder decoder = new H264Decoder();
                Picture buf = Picture.create(1920, 1088, ColorSpace.YUV420);
                Picture pic;
                int i = 0;
                while ((pic = decoder.decodeFrame(reorder(demuxer.nextFrame()), buf.getData())) != null) {
                    if (rawReader == null || oldWidth != pic.getWidth() || oldHeight != pic.getHeight()) {
                        rawReader = new RawReader(decoded, pic.getWidth(), pic.getHeight());
                        oldWidth = pic.getWidth();
                        oldHeight = pic.getHeight();
                    }

                    Picture ref = rawReader.readNextFrame();
                    if (!compare(ref, pic)) {
                        System.err.println(" - FAILED (" + i + ")");
                        sb.append("FAILED");
                        return;
                    }
                    i++;
                }
                Picture ref = rawReader.readNextFrame();
                if (ref != null) {
                    System.err.println(" - FAILED");
                    sb.append("FAILED");
                } else {
                    System.out.println(" - PASSED");
                    sb.append("<span class=\"passed\">PASSED</span>");
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
        } catch (Throwable t) {
            System.err.println(" - FAILED");
            sb.append("FAILED");
            t.printStackTrace();
        }
    }

    private ByteBuffer reorder(Packet nextFrame) {
        throw new RuntimeException("Display order reordering!!!");
    }

    private static boolean compare(Picture expected, Picture actual) {

        int size = expected.getWidth() * expected.getHeight();

        int[] expY = expected.getPlaneData(0);
        int[] actY = actual.getPlaneData(0);
        for (int i = 0; i < size; i++) {
            if (expY[i] != actY[i])
                return false;
        }

        int[] expCb = expected.getPlaneData(1);
        int[] actCb = actual.getPlaneData(1);
        for (int i = 0; i < (size >> 2); i++) {
            if (expCb[i] != actCb[i])
                return false;
        }

        int[] expCr = expected.getPlaneData(2);
        int[] actCr = actual.getPlaneData(2);
        for (int i = 0; i < (size >> 2); i++) {
            if (expCr[i] != actCr[i])
                return false;
        }

        return true;
    }
}
