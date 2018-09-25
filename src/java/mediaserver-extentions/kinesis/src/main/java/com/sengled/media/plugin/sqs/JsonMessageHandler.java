package com.sengled.media.plugin.sqs;

import java.util.Map;

public interface JsonMessageHandler {
    public void handle(Map<String, Object> json);
}
