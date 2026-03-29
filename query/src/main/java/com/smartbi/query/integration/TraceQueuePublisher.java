package com.smartbi.query.integration;

public interface TraceQueuePublisher {
    void publish(String listKey, String payload);
}
