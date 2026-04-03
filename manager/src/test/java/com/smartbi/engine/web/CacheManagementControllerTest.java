package com.smartbi.engine.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CacheManagementControllerTest {

    private final StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CacheManagementController(redisTemplate))
            .setControllerAdvice(new RestExceptionHandler())
            .build();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    // Covers CacheManagementController#getCacheInfo empty scan branch.
    void getCacheInfoReturnsEmptyWhenNoKeys() throws Exception {
        mockScan();

        mockMvc.perform(get("/api/v1/cache/info").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyCount").value(0))
                .andExpect(jsonPath("$.totalSizeBytes").value(0));
    }

    @Test
    // Covers CacheManagementController#getCacheInfo aggregation branch.
    void getCacheInfoReturnsTotalSizeAndCount() throws Exception {
        mockScan("kylin_cache:key1", "kylin_cache:key2");
        when(valueOperations.size("kylin_cache:key1")).thenReturn(6L);
        when(valueOperations.size("kylin_cache:key2")).thenReturn(8L);

        mockMvc.perform(get("/api/v1/cache/info").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyCount").value(2))
                .andExpect(jsonPath("$.totalSizeBytes").value(14));
    }

    @Test
    // Covers CacheManagementController#listCacheKeys listing branch.
    void listCacheKeysReturnsAllKeys() throws Exception {
        mockScan("kylin_cache:key1", "kylin_cache:key2");
        when(valueOperations.size("kylin_cache:key1")).thenReturn(6L);
        when(valueOperations.size("kylin_cache:key2")).thenReturn(7L);
        when(redisTemplate.getExpire("kylin_cache:key1")).thenReturn(300L);
        when(redisTemplate.getExpire("kylin_cache:key2")).thenReturn(301L);

        mockMvc.perform(get("/api/v1/cache/keys").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].key").value("kylin_cache:key1"))
                .andExpect(jsonPath("$[1].key").value("kylin_cache:key2"));
    }

    @Test
    // Covers CacheManagementController#listCacheKeys pagination branch.
    void listCacheKeysSupportsPagination() throws Exception {
        mockScan("kylin_cache:key1", "kylin_cache:key2", "kylin_cache:key3");
        when(valueOperations.size(any())).thenReturn(6L);
        when(redisTemplate.getExpire(any())).thenReturn(300L);

        mockMvc.perform(get("/api/v1/cache/keys")
                        .param("offset", "1")
                        .param("limit", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].key").value("kylin_cache:key2"));
    }

    @Test
    // Covers CacheManagementController#getCacheKey success path.
    void getCacheKeyReturnsDetailWhenPresent() throws Exception {
        when(redisTemplate.hasKey("kylin_cache:key1")).thenReturn(true);
        when(valueOperations.get("kylin_cache:key1")).thenReturn("{\"rows\":1}");
        when(valueOperations.size("kylin_cache:key1")).thenReturn(10L);
        when(redisTemplate.getExpire("kylin_cache:key1")).thenReturn(120L);

        mockMvc.perform(get("/api/v1/cache/keys/kylin_cache:key1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("kylin_cache:key1"))
                .andExpect(jsonPath("$.value").value("{\"rows\":1}"))
                .andExpect(jsonPath("$.sizeBytes").value(10))
                .andExpect(jsonPath("$.ttlSeconds").value(120));
    }

    @Test
    // Covers CacheManagementController#createCacheKey success path.
    void createCacheKeyCreatesNewEntry() throws Exception {
        when(redisTemplate.hasKey("kylin_cache:new")).thenReturn(false);
        doNothing().when(valueOperations).set(eq("kylin_cache:new"), eq("{\"ok\":true}"), eq(Duration.ofSeconds(90)));

        mockMvc.perform(post("/api/v1/cache/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"kylin_cache:new\",\"value\":\"{\\\"ok\\\":true}\",\"ttlSeconds\":90}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("kylin_cache:new"))
                .andExpect(jsonPath("$.ttlSeconds").value(90))
                .andExpect(jsonPath("$.sizeBytes").value(11));

        verify(valueOperations).set("kylin_cache:new", "{\"ok\":true}", Duration.ofSeconds(90));
    }

    @Test
    // Covers CacheManagementController#createCacheKey duplicate-key rejection.
    void createCacheKeyRejectsDuplicateKey() throws Exception {
        when(redisTemplate.hasKey("kylin_cache:new")).thenReturn(true);

        mockMvc.perform(post("/api/v1/cache/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"kylin_cache:new\",\"value\":\"{}\",\"ttlSeconds\":90}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("cache key already exists: kylin_cache:new"));
    }

    @Test
    // Covers CacheManagementController#createCacheKey invalid-prefix validation.
    void createCacheKeyRejectsInvalidPrefix() throws Exception {
        mockMvc.perform(post("/api/v1/cache/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"other:key\",\"value\":\"{}\",\"ttlSeconds\":90}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("cache key must start with kylin_cache: and must not contain '/' or whitespace"));
    }

    @Test
    // Covers CacheManagementController#createCacheKey invalid-ttl validation.
    void createCacheKeyRejectsInvalidTtl() throws Exception {
        mockMvc.perform(post("/api/v1/cache/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"kylin_cache:new\",\"value\":\"{}\",\"ttlSeconds\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ttlSeconds must be greater than 0"));
    }

    @Test
    // Covers CacheManagementController#updateCacheKey success path.
    void updateCacheKeyUpdatesExistingEntry() throws Exception {
        when(redisTemplate.hasKey("kylin_cache:key1")).thenReturn(true);
        doNothing().when(valueOperations).set(eq("kylin_cache:key1"), eq("{\"rows\":2}"), eq(Duration.ofSeconds(45)));

        mockMvc.perform(put("/api/v1/cache/keys/kylin_cache:key1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"{\\\"rows\\\":2}\",\"ttlSeconds\":45}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("kylin_cache:key1"))
                .andExpect(jsonPath("$.ttlSeconds").value(45))
                .andExpect(jsonPath("$.sizeBytes").value(10));

        verify(valueOperations).set("kylin_cache:key1", "{\"rows\":2}", Duration.ofSeconds(45));
    }

    @Test
    // Covers CacheManagementController#updateCacheKey missing-key branch.
    void updateCacheKeyReturnsNotFoundWhenKeyIsMissing() throws Exception {
        when(redisTemplate.hasKey("kylin_cache:missing")).thenReturn(false);

        mockMvc.perform(put("/api/v1/cache/keys/kylin_cache:missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"{}\",\"ttlSeconds\":45}"))
                .andExpect(status().isNotFound());
    }

    @Test
    // Covers CacheManagementController#deleteCacheKey deletion branch.
    void deleteCacheKeyRemovesKey() throws Exception {
        mockMvc.perform(delete("/api/v1/cache/keys/kylin_cache:key1"))
                .andExpect(status().isNoContent());

        verify(redisTemplate).delete("kylin_cache:key1");
    }

    @Test
    // Covers CacheManagementController#deleteCacheKey ignore branch.
    void deleteCacheKeyIgnoresNonCacheKeys() throws Exception {
        mockMvc.perform(delete("/api/v1/cache/keys/other:key"))
                .andExpect(status().isNoContent());

        verify(redisTemplate, never()).delete(anyString());
    }

    @SuppressWarnings("unchecked")
    private void mockScan(String... keys) {
        Cursor<String> cursor = Mockito.mock(Cursor.class);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

        Boolean[] hasNext = new Boolean[keys.length + 1];
        for (int i = 0; i < keys.length; i++) {
            hasNext[i] = Boolean.TRUE;
        }
        hasNext[keys.length] = Boolean.FALSE;
        when(cursor.hasNext()).thenReturn(hasNext[0], slice(hasNext));
        if (keys.length > 0) {
            when(cursor.next()).thenReturn(keys[0], slice(keys));
        }
    }

    private Boolean[] slice(Boolean[] values) {
        if (values.length <= 1) {
            return new Boolean[0];
        }
        Boolean[] copy = new Boolean[values.length - 1];
        System.arraycopy(values, 1, copy, 0, values.length - 1);
        return copy;
    }

    private String[] slice(String[] values) {
        if (values.length <= 1) {
            return new String[0];
        }
        String[] copy = new String[values.length - 1];
        System.arraycopy(values, 1, copy, 0, values.length - 1);
        return copy;
    }
}
