package com.sengled.media.server.rtsp.rtp.packetizer.h264;

import java.nio.ByteBuffer;
import org.apache.commons.codec.binary.Base64;
import junit.framework.TestCase;

public class TestMediaDescriptionParser extends TestCase {

    public void testParseExtra() {
        byte[] sps = Base64.decodeBase64("Z00AH5WoFAFuQA==");
        byte[] pps = Base64.decodeBase64("aO48gA==");
        byte[] profile = new byte[]{0x4D, 0x00, 0x1F};
        
       AVCDecoderConfigurationRecord record = new AVCDecoderConfigurationRecord();
       record.setSPS_PPS(ByteBuffer.wrap(sps), ByteBuffer.wrap(pps), ByteBuffer.wrap(profile));
    }
}
