package com.sengled.media.bootstrap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RestResultBuilder  {
    private static final String SERVER_NOT_AVALIABLE = "ServerNotAvaliable";
    private static final String KEY_STATUS = "status";
    private static final String KEY_DATA = "data";
    private static final String VALUE_OK = "ok";
    private static final String VALUE_STREAM_NOT_FOUND = "StreamNotFound";
    private static final String KEY_DATAS = "datas";

    private Map<String, Object> values = new HashMap<>();
    
    public RestResultBuilder with(String key, Object value) {
        values.put(key, value);
        
        return this;
    }

    public Map<String, Object> streamNotFound() {
        return withStatus(VALUE_STREAM_NOT_FOUND).unmodifiableValues();
    }


    public Map<String, Object> serverNotAvaliable() {
        return withStatus(SERVER_NOT_AVALIABLE).unmodifiableValues();
    }
    
    public Map<String, Object> ok() {
        return withStatus(VALUE_OK).unmodifiableValues();
    }

    
    private Map<String, Object> unmodifiableValues() {
        return Collections.unmodifiableMap(values);
    }

    public RestResultBuilder withStatus(String status) {
        with(KEY_STATUS, status);
        
        return this;
    } 

    public RestResultBuilder withData(Object data) {
        with(KEY_DATA,  data);

        return this;
    } 
    
    public RestResultBuilder withDatas(Object data) {
        with(KEY_DATAS,  data);

        return this;
    } 
    
    public static RestResultBuilder newInstance() {
        return new RestResultBuilder();
    }

}
