package com.smartbi.query.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.query.api.dto.SqlResponseStubDto;
import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.integration.QueryCacheStore;
import com.smartbi.query.parsing.SqlCommentParser;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class QueryCacheServiceTest {

    // Covers QueryCacheService#buildKey datasource-isolated branch.
    @Test
    void shouldBuildDifferentKeysForDifferentDatasourcesWhenIsolationEnabled() {
        QueryProperties properties = new QueryProperties();
        properties.getCache().setDatasourceIsolationEnabled(true);
        QueryCacheService service = new QueryCacheService(new MapCacheStore(), properties, new ObjectMapper());
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse("SELECT * FROM sales");

        String key1 = service.buildKey(parsed, "", "default");
        String key2 = service.buildKey(parsed, "", "presto_local");

        assertNotEquals(key1, key2);
    }

    // Covers QueryCacheService#tryGet and QueryCacheService#put happy-path JSON round-trip.
    @Test
    void shouldRoundTripCachedResponse() {
        QueryProperties properties = new QueryProperties();
        MapCacheStore store = new MapCacheStore();
        QueryCacheService service = new QueryCacheService(store, properties, new ObjectMapper());
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse("SELECT * FROM sales");

        SqlResponseStubDto response = new SqlResponseStubDto();
        response.setCube("default");
        response.setDuration(12);
        response.setResults(java.util.Collections.singletonList(new String[]{"1", "foo"}));

        service.put(parsed, "", "default", response);
        SqlResponseStubDto cached = service.tryGet(parsed, "", "default");

        assertNotNull(cached);
        assertEquals("default", cached.getCube());
        assertEquals(1, cached.getResults().size());
        assertEquals(300, store.ttlForLastSet());
    }

    // Covers QueryCacheService#buildKey shared-cache and explicit cache-key branches.
    @Test
    void shouldBuildSharedKeysAndHonorExplicitCacheKey() {
        QueryProperties properties = new QueryProperties();
        properties.getCache().setDatasourceIsolationEnabled(false);
        QueryCacheService service = new QueryCacheService(new MapCacheStore(), properties, new ObjectMapper());

        SqlCommentParser.ParsedSql shared = SqlCommentParser.parse("SELECT * FROM sales");
        String sharedDefault = service.buildKey(shared, "fp", "default");
        String sharedPresto = service.buildKey(shared, "fp", "presto_local");

        assertEquals(sharedDefault, sharedPresto);

        SqlCommentParser.ParsedSql keyed = SqlCommentParser.parse("/* cache-key=inventory */ SELECT * FROM sales");
        assertEquals("kylin_cache:shared:inventory", service.buildKey(keyed, "fp", "default"));
    }

    // Covers QueryCacheService#effectiveTtl default and override branches.
    @Test
    void shouldResolveEffectiveTtlFromMetadataOverrideOrDefault() {
        QueryProperties properties = new QueryProperties();
        properties.getCache().setDefaultTtlSeconds(42);
        QueryCacheService service = new QueryCacheService(new MapCacheStore(), properties, new ObjectMapper());

        assertEquals(42, service.effectiveTtl(SqlCommentParser.parse("SELECT * FROM sales")));
        assertEquals(60, service.effectiveTtl(SqlCommentParser.parse("/* cache-ttl=60 */ SELECT * FROM sales")));
    }

    // Covers QueryCacheService#put oversized-payload skip branch.
    @Test
    void shouldSkipCachingOversizedResponses() {
        QueryProperties properties = new QueryProperties();
        properties.getCache().setMaxCacheSizeBytes(8);
        MapCacheStore store = new MapCacheStore();
        QueryCacheService service = new QueryCacheService(store, properties, new ObjectMapper());

        SqlResponseStubDto response = new SqlResponseStubDto();
        response.setCube("default");
        response.setResults(java.util.Collections.singletonList(new String[]{"alpha"}));

        service.put(SqlCommentParser.parse("SELECT * FROM sales"), "", "default", response);

        assertNull(store.lastKey());
    }

    // Covers QueryCacheService#tryGet deserialization failure fallback branch.
    @Test
    void shouldReturnNullWhenCachedPayloadCannotBeDeserialized() {
        QueryProperties properties = new QueryProperties();
        MapCacheStore store = new MapCacheStore();
        QueryCacheService service = new QueryCacheService(store, properties, new ObjectMapper());
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse("SELECT * FROM sales");

        store.putRaw(service.buildKey(parsed, "", "default"), "{not-json");

        assertNull(service.tryGet(parsed, "", "default"));
    }

    private static class MapCacheStore implements QueryCacheStore {
        private final Map<String, String> values = new HashMap<String, String>();
        private String lastKey;
        private int lastTtl;

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public void set(String key, String value, int ttlSeconds) {
            lastKey = key;
            lastTtl = ttlSeconds;
            values.put(key, value);
        }

        private void putRaw(String key, String value) {
            values.put(key, value);
        }

        private String lastKey() {
            return lastKey;
        }

        private int ttlForLastSet() {
            return lastTtl;
        }
    }
}
