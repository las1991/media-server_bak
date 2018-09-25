package com.sengled.media.event;

import com.sengled.media.MediaSource;
import com.sengled.media.server.rtsp.RtspServerContext;

/**
 * rtsp session 事件
 * 
 * @author 陈修恒
 * @date 2016年5月3日
 */
public final class SessionEvent implements Event {
    private final RtspServerContext context;
    private final EventType eventType;
    private final MediaSource source;

    /**
     * 设备上线
     * @param source
     * @return
     */
    public static SessionEvent newOnlineEvent(MediaSource source) {
        return new SessionEvent(EventType.ONLINE, source);
    }
    
    public static Event newEvent(MediaSource source, EventType event) {
        return new SessionEvent(event, source);
    }
    
    /**
     * 设备下线
     * @param source
     * @return
     */
    public static SessionEvent newOfflineEvent(MediaSource source) {
        return new SessionEvent(EventType.OFFLINE, source);
    }


    public static Event newDiscontinuedEvent(MediaSource source) {
        return new SessionEvent(EventType.DISCONTINUED, source);
    }
    
    private SessionEvent(EventType eventType, MediaSource source) {
        this.context = source.getServerContext();
        this.eventType = eventType;
        this.source = source;
    }

    public MediaSource getSource() {
        return source;
    }
    
    public String getToken() {
        return source.getToken();
    }
    
    @Override
    public EventType getEventType() {
        return eventType;
    }
    

    public RtspServerContext getServerContext() {
        return context;
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{").append(getClass().getSimpleName());
        buf.append(", type=").append(getEventType());
        buf.append(", src=").append(source);
        buf.append("}");
        return buf.toString();
    }

}
