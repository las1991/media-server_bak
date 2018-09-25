package com.sengled.media.url;


public class TestURLObject extends junit.framework.TestCase{

    public void testParse() {
        URLObject url ;
        
        url = URLObject.parse("rtsp://localhost:554/a.sdp");
        assertEquals("rtsp://localhost:554/a.sdp", url.getUrl());
        

        url = URLObject.parse("rtsp://localhost:554/a.sdp?k=v");
        assertEquals("rtsp://localhost:554/a.sdp", url.getUrl());
        assertEquals("k=v", url.getQueryString());
        assertEquals("v", url.getParameter("k"));
    }
    
}
