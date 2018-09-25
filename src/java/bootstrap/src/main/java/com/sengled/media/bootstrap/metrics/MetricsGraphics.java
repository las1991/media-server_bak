package com.sengled.media.bootstrap.metrics;

public interface MetricsGraphics {

    Graphics getOrCreateGraphics(String name,
                   String type,
                   String colTemplates);
    

}
