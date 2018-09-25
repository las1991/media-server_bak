package com.sengled.media.event;

public enum EventType {
    /** 上线 */
    ONLINE,
    /** 下线 */
    OFFLINE,
    /** 流重置流了 **/
    DISCONTINUED,
    
    /** 录像中断 **/
    RECORDER_INTERRUPTED,
    
    HeartBeat;

    private String name;
    
    private EventType() {
        this.name = name().toLowerCase();
    }

    public boolean isOnline() {
        return this == ONLINE;
    }

    public boolean isOffline() {
        return this == OFFLINE;
    }
    
    
    @Override
    public String toString() {
        return name;
    }
}
