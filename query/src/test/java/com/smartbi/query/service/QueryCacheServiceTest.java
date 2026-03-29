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

class QueryCacheServiceTest {

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
    }

    private static class MapCacheStore implements QueryCacheStore {
        private final Map<String, String> values = new HashMap<String, String>();

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public void set(String key, String value, int ttlSeconds) {
            values.put(key, value);
        }
    }
}
