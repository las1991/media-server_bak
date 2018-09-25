package com.sengled.media.plugin.config.storage;

import java.util.TimeZone;

/**
 * Created by las on 2017/3/23.
 */

/**
 * storage: {
 * enable: true,          // 开了存储服务
 * timeZone:’America/Los_Angeles’, // 设备所在时区，考虑到夏令时问题，请使用城市时区
 * fileExpires: 7         // 录像保存 7 天
 * }
 */
public class StorageConfig {
    private boolean enable;
    private int fileExpires;
    private String timeZone = TimeZone.getDefault().getID();

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public int getFileExpires() {
        return fileExpires;
    }

    public void setFileExpires(int fileExpires) {
        this.fileExpires = fileExpires;
    }
    
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
    
    public String getTimeZone() {
        return timeZone;
    }
}
