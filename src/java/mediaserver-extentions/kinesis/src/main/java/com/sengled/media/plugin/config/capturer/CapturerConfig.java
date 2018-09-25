package com.sengled.media.plugin.config.capturer;

/**
 * Created by las on 2017/3/23.
 */

/**
 * screenshot:{
 * enable: true,           // 开启了截图服务
 * intervalSeconds:120    // 截图间隔(单位:秒)，默认 2min
 * }
 */
public class CapturerConfig {
    private boolean enable;
    private int intervalSeconds;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }
}
