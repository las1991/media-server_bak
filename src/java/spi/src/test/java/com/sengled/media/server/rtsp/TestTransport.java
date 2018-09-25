package com.sengled.media.server.rtsp;

import junit.framework.TestCase;

public class TestTransport extends TestCase {

    public void testDecode() {
        String cmd = "RTP/AVP/TCP;unicast;mode=receive;interleaved=2-3";
        Transport transport = Transport.parse(cmd);
        
        assertEquals("RTP/AVP/TCP", transport.getTranport());
        assertEquals("unicast", transport.getUnicast());
        assertEquals("receive", transport.getMode());
        assertEquals(2, transport.getRtpChannel());
        assertEquals(3, transport.getRtcpChannel());
        
        
        cmd = "RTP/AVP/TCP;unicast;interleaved=2-3;mode=receive;";
        transport = Transport.parse(cmd);
        
        assertEquals("RTP/AVP/TCP", transport.getTranport());
        assertEquals("unicast", transport.getUnicast());
        assertEquals("receive", transport.getMode());
        assertEquals(2, transport.getRtpChannel());
        assertEquals(3, transport.getRtcpChannel());
        System.out.println(transport);
    }
}
