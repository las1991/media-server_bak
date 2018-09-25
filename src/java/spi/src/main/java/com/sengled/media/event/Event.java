package com.sengled.media.event;

/**
 * 通过 EventBus 抛出来的事件 
 * 
 * @author chenxh
 */
public interface Event {
    EventType getEventType();
}
