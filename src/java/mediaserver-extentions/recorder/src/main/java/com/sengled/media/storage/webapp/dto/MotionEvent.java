package com.sengled.media.storage.webapp.dto;

public class MotionEvent {

    private String token;
    private String timeStamp; // format: "yyyy-MM-dd HH:mm:ss"
    private String resource_type;
    private Integer motion_storage_time;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getResource_type() {
        return resource_type;
    }

    public void setResource_type(String resource_type) {
        this.resource_type = resource_type;
    }

    public Integer getMotion_storage_time() {
        return motion_storage_time;
    }

    public void setMotion_storage_time(Integer motion_storage_time) {
        this.motion_storage_time = motion_storage_time;
    }

    @Override
    public String toString() {
        return "MotionEvent [token=" + token + ", timeStamp=" + timeStamp + ", resource_type="
                + resource_type + ", motion_storage_time=" + motion_storage_time + "]";
    }



}
