package com.sengled.media.clust;

import java.util.HashMap;
import java.util.Map;

public class StringBytesMap extends HashMap<String, byte[]> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    
    public void put(byte[] key, byte[] value) {
        super.put(new String(key), value);
    }
    
    public void put(String key, String value) {
        super.put(key, value.getBytes());
    }
    
    public void put(String key, long value) {
        super.put(key, String.valueOf(value).getBytes());
    }
    
    public String getString(String key) {
        byte[] value = get(key);
        if (null != value) {
            return new String(value);
        }
        
        return null;
    }
    
    public int getInt(String key) {
        byte[] value = get(key);
        if (null != value) {
            return Integer.parseInt(new String(value));
        }
        
        return 0;
    }
    
    public long getLong(String key) {
        byte[] value = get(key);
        if (null != value) {
            return Long.parseLong(new String(value));
        }
        
        return 0;
    }
    
    public Map<byte[], byte[]> toByteMap() {
        HashMap<byte[], byte[]> bytesMap = new HashMap<>();
        
        for (java.util.Map.Entry<String, byte[]> entry : entrySet()) {
            if (null != entry.getValue()) {
                bytesMap.put(entry.getKey().getBytes(), entry.getValue());
            }
        }
        
        return bytesMap;
    }
}
