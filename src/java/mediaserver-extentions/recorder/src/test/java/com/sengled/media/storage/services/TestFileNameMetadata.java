package com.sengled.media.storage.services;

import junit.framework.TestCase;

public class TestFileNameMetadata extends TestCase {
    public void testParseWithoutUserId() {
        // parse
        String name = "79DA565108383D69CD94ED9F411D37FA_FS_1533177346064_FS_1533177406727_FS_168_FS_Australia$Melbourne_FS_991.flv";
        FileNameMetadata metadata = FileNameMetadata.parse(name);
        
        assertEquals("79DA565108383D69CD94ED9F411D37FA", metadata.getToken());
        assertEquals(1533177346064L, metadata.getStartTime());
        assertEquals(1533177406727L, metadata.getEndTime());
        assertEquals(168, metadata.getStorageTTLHours());
        assertEquals("Australia/Melbourne", metadata.getTimeZoneCity());
        assertEquals(991, metadata.getVideoIndex());
        assertNull(metadata.getUserId());
        
        // encode
        assertEquals(name, metadata.encode());
        
    }
    
    
    public void testParseWithUserId() {
        // parse
        String name = "79DA565108383D69CD94ED9F411D37FA_FS_1533177346064_FS_1533177406727_FS_168_FS_Australia$Melbourne_FS_991_FS_8.flv";
        FileNameMetadata metadata = FileNameMetadata.parse(name);
        
        assertEquals("79DA565108383D69CD94ED9F411D37FA", metadata.getToken());
        assertEquals(1533177346064L, metadata.getStartTime());
        assertEquals(1533177406727L, metadata.getEndTime());
        assertEquals(168, metadata.getStorageTTLHours());
        assertEquals("Australia/Melbourne", metadata.getTimeZoneCity());
        assertEquals(991, metadata.getVideoIndex());
        assertEquals(8, metadata.getUserId().longValue());

        // encode
        assertEquals(name, metadata.encode());
    }
}
