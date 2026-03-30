package com.smartbi.query.support;

import com.smartbi.query.integration.QueryCacheStore;
import com.smartbi.query.integration.TraceQueuePublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryQueryInfrastructure implements QueryCacheStore, TraceQueuePublisher {

    private final Map<String, String> cache = new ConcurrentHashMap<String, String>();
    private final List<String> traces = new ArrayList<String>();

    @Override
    public String get(String key) {
        return cache.get(key);
    }

    @Override
    public void set(String key, String value, int ttlSeconds) {
        cache.put(key, value);
    }

    @Override
    public synchronized void publish(String listKey, String payload) {
        traces.add(payload);
    }

    public void clear() {
        cache.clear();
        synchronized (this) {
            traces.clear();
        }
    }

    public synchronized int traceCount() {
        return traces.size();
    }

    public synchronized List<String> publishedTraces() {
        return new ArrayList<String>(traces);
    }
}
