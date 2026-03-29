package com.smartbi.query.integration;

public interface QueryCacheStore {
    String get(String key);

    void set(String key, String value, int ttlSeconds);
}
