package com.sengled.media.storage.services;

import com.alibaba.fastjson.annotation.JSONField;

public class StorageMessage {
    @JSONField(name = "token")
    private String token;

    @JSONField(name = "userId")
    private long userId;
    
    @JSONField(name = "startTime")
    private long startTime;

    @JSONField(name = "endTime")
    private long endTime;

    @JSONField(name = "liveHours")
    private int liveHours;

    @JSONField(name = "flvFileName")
    private String videoFile;

    @JSONField(name = "position")
    private long position;

    @JSONField(name = "videoIndex")
    private long videoIndex;
    
    @JSONField(name = "length")
    private long length;

    @JSONField(name = "city")
    private String city;


    public int getStorageTimeDays() {
        return liveHours % 24 == 0 ? (liveHours / 24) : (liveHours / 24 + 1);
    }


    public String getToken() {
        return token;
    }


    public void setToken(String token) {
        this.token = token;
    }


    public long getStartTime() {
        return startTime;
    }


    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }


    public long getEndTime() {
        return endTime;
    }


    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }


    public int getLiveHours() {
        return liveHours;
    }


    public void setLiveHours(int liveHours) {
        this.liveHours = liveHours;
    }


    public String getVideoFile() {
        return videoFile;
    }


    public void setVideoFile(String videoFile) {
        this.videoFile = videoFile;
    }


    public long getPosition() {
        return position;
    }


    public void setPosition(long position) {
        this.position = position;
    }


    public long getVideoIndex() {
        return videoIndex;
    }


    public void setVideoIndex(long videoIndex) {
        this.videoIndex = videoIndex;
    }


    public long getLength() {
        return length;
    }


    public void setLength(long length) {
        this.length = length;
    }


    public String getCity() {
        return city;
    }


    public void setCity(String city) {
        this.city = city;
    }


    public long getUserId() {
        return userId;
    }


    public void setUserId(long userId) {
        this.userId = userId;
    }


    @Override
    public String toString() {
        return "StorageMessage [token=" + token + ", userId=" + userId + ", startTime=" + startTime + ", endTime=" + endTime + ", liveHours="
                + liveHours + ", videoFile=" + videoFile + ", position=" + position + ", videoIndex=" + videoIndex + ", length=" + length + ", city="
                + city + "]";
    }
}
