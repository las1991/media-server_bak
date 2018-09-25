package com.sengled.media.event;

import com.google.common.eventbus.Subscribe;

public interface EventBusListener<T extends Event> {
    @Subscribe
    public void onEvent(T event);
}
