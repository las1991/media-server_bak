package com.sengled.media.clust.server;

public class MediaServerRuntime {
    private double cpuLoad;
    private double cpuIdle;
    private double memory;
    
    private int deviceNum;

    public MediaServerRuntime() {
    }

    public double getCpuLoad() {
        return cpuLoad;
    }

    public void setCpuLoad(double cpuLoad) {
        this.cpuLoad = cpuLoad;
    }


    public double getMemory() {
        return memory;
    }

    public void setMemory(double memory) {
        this.memory = memory;
    }
    
    public void setDeviceNum(int deviceNum) {
        this.deviceNum = deviceNum;
    }
    
    public int getDeviceNum() {
        return deviceNum;
    }
    
    public void setCpuIdle(double cpuIdle) {
        this.cpuIdle = cpuIdle;
    }
    
    public double getCpuIdle() {
        return cpuIdle;
    }
}
