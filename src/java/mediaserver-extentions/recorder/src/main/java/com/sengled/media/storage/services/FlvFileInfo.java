package com.sengled.media.storage.services;

public class FlvFileInfo {

    public FlvFileInfo(String token, long userId, long startTime, long endTime, String flvFileName, int liveHours, String city, long videoIndex,
            long position, long length) {
        super();
        this.token = token;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.flvFileName = flvFileName;
        this.liveHours = liveHours;
        this.city = city;
        this.videoIndex = videoIndex;
        this.position = position;
        this.length = length;
    }

    private String token;
    private long userId;
    private long startTime;
    private long endTime;
    private String flvFileName;
    private int liveHours;
    private String city;
    private long videoIndex;
    long position;
    long length;

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

    public String getFlvFileName() {
        return flvFileName;
    }

    public void setFlvFileName(String flvFileName) {
        this.flvFileName = flvFileName;
    }

    public int getLiveHours() {
        return liveHours;
    }

    public void setLiveHours(int liveHours) {
        this.liveHours = liveHours;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public long getVideoIndex() {
        return videoIndex;
    }

    public void setVideoIndex(long videoIndex) {
        this.videoIndex = videoIndex;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }


}
