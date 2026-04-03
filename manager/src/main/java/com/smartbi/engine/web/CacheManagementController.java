package com.smartbi.engine.web;

import com.smartbi.engine.web.dto.CacheInfoDto;
import com.smartbi.engine.web.dto.CacheKeyDetailDto;
import com.smartbi.engine.web.dto.CacheKeyDto;
import com.smartbi.engine.web.dto.CacheKeyPageDto;
import com.smartbi.engine.web.dto.CacheKeyUpsertRequest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cache")
public class CacheManagementController {

    private final StringRedisTemplate redisTemplate;
    private static final String CACHE_KEY_PREFIX = "kylin_cache:";
    private static final String SCAN_COMMAND = "SCAN";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int SCAN_BATCH_SIZE = 100;
    private static final String SUMMARY_MESSAGE = "Exact live cache totals are disabled; enter a narrower managed-key prefix to inspect Redis keys with bounded scan cost.";

    public CacheManagementController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/info")
    public CacheInfoDto getCacheInfo() {
        CacheInfoDto info = new CacheInfoDto();
        info.setManagedKeyPrefix(CACHE_KEY_PREFIX);
        info.setExactSummaryAvailable(false);
        info.setSummaryMessage(SUMMARY_MESSAGE);
        return info;
    }

    @GetMapping("/keys")
    public CacheKeyPageDto listCacheKeys(@RequestParam(required = false) String prefix,
                                         @RequestParam(defaultValue = "0") String cursor,
                                         @RequestParam(defaultValue = "20") int limit) {
        String normalizedPrefix = validateSearchPrefix(prefix);
        String normalizedCursor = normalizeCursor(cursor);
        int boundedLimit = clampLimit(limit);

        List<CacheKeyDto> items = new ArrayList<CacheKeyDto>();
        String currentCursor = normalizedCursor;
        boolean hasMore = false;
        while (items.size() < boundedLimit) {
            ScanChunk chunk = scanChunk(currentCursor, normalizedPrefix);
            currentCursor = chunk.getNextCursor();
            for (String key : chunk.getKeys()) {
                items.add(toKeyDto(key));
                if (items.size() >= boundedLimit) {
                    break;
                }
            }
            if (items.size() >= boundedLimit) {
                hasMore = !"0".equals(currentCursor);
                break;
            }
            if ("0".equals(currentCursor)) {
                hasMore = false;
                break;
            }
        }

        CacheKeyPageDto page = new CacheKeyPageDto();
        page.setItems(items);
        page.setQueryPrefix(normalizedPrefix);
        page.setHasMore(hasMore);
        page.setNextCursor(hasMore ? currentCursor : "0");
        return page;
    }

    @GetMapping("/keys/{key}")
    public ResponseEntity<CacheKeyDetailDto> getCacheKey(@PathVariable String key) {
        validateCacheKey(key);
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return ResponseEntity.notFound().build();
        }
        String value = redisTemplate.opsForValue().get(key);
        return ResponseEntity.ok(toDetailDto(key, value));
    }

    @PostMapping("/keys")
    public ResponseEntity<CacheKeyDetailDto> createCacheKey(@RequestBody CacheKeyUpsertRequest request) {
        String key = requireValidCreateRequest(request);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            throw new IllegalStateException("cache key already exists: " + key);
        }
        long ttlSeconds = requireValidTtl(request.getTtlSeconds());
        redisTemplate.opsForValue().set(key, request.getValue(), Duration.ofSeconds(ttlSeconds));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDetailDto(key, request.getValue(), ttlSeconds));
    }

    @PutMapping("/keys/{key}")
    public ResponseEntity<CacheKeyDetailDto> updateCacheKey(@PathVariable String key,
                                                            @RequestBody CacheKeyUpsertRequest request) {
        validateCacheKey(key);
        requireValidValue(request.getValue());
        long ttlSeconds = requireValidTtl(request.getTtlSeconds());
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return ResponseEntity.notFound().build();
        }
        redisTemplate.opsForValue().set(key, request.getValue(), Duration.ofSeconds(ttlSeconds));
        return ResponseEntity.ok(toDetailDto(key, request.getValue(), ttlSeconds));
    }

    @DeleteMapping("/keys/{key}")
    public ResponseEntity<Void> deleteCacheKey(@PathVariable String key) {
        if (isCacheKeyCandidate(key)) {
            redisTemplate.delete(key);
        }
        return ResponseEntity.noContent().build();
    }

    private CacheKeyDetailDto toDetailDto(String key, String value) {
        CacheKeyDetailDto dto = new CacheKeyDetailDto();
        dto.setKey(key);
        dto.setValue(value);
        Long size = redisTemplate.opsForValue().size(key);
        dto.setSizeBytes(size != null ? size : 0L);
        Long ttl = redisTemplate.getExpire(key);
        dto.setTtlSeconds(ttl != null ? ttl : -1L);
        return dto;
    }

    private CacheKeyDetailDto toDetailDto(String key, String value, long ttlSeconds) {
        CacheKeyDetailDto dto = new CacheKeyDetailDto();
        dto.setKey(key);
        dto.setValue(value);
        dto.setSizeBytes(value == null ? 0L : value.getBytes(StandardCharsets.UTF_8).length);
        dto.setTtlSeconds(ttlSeconds);
        return dto;
    }

    private CacheKeyDto toKeyDto(String key) {
        CacheKeyDto dto = new CacheKeyDto();
        dto.setKey(key);
        Long size = redisTemplate.opsForValue().size(key);
        dto.setSizeBytes(size != null ? size : 0L);
        Long ttl = redisTemplate.getExpire(key);
        dto.setTtlSeconds(ttl != null ? ttl : -1L);
        return dto;
    }

    private String requireValidCreateRequest(CacheKeyUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("cache request is required");
        }
        validateCacheKey(request.getKey());
        requireValidValue(request.getValue());
        return request.getKey();
    }

    private void validateCacheKey(String key) {
        if (!isCacheKeyCandidate(key)) {
            throw new IllegalArgumentException("cache key must start with kylin_cache: and must not contain '/' or whitespace");
        }
    }

    private boolean isCacheKeyCandidate(String key) {
        return key != null
                && key.startsWith(CACHE_KEY_PREFIX)
                && key.indexOf('/') < 0
                && !key.chars().anyMatch(Character::isWhitespace);
    }

    private String validateSearchPrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("cache key prefix is required");
        }
        String normalized = prefix.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("cache key prefix is required");
        }
        if (!isCacheKeyCandidate(normalized)) {
            throw new IllegalArgumentException("cache key prefix must start with kylin_cache: and must not contain '/' or whitespace");
        }
        if (CACHE_KEY_PREFIX.equals(normalized)) {
            throw new IllegalArgumentException("cache key prefix must be narrower than kylin_cache:");
        }
        return normalized;
    }

    private String normalizeCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return "0";
        }
        String normalized = cursor.trim();
        try {
            long parsed = Long.parseLong(normalized);
            if (parsed < 0L) {
                throw new IllegalArgumentException("cursor must be a non-negative number");
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("cursor must be a non-negative number");
        }
        return normalized;
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private ScanChunk scanChunk(String cursor, String prefix) {
        Object rawResult = redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                SCAN_COMMAND,
                cursor.getBytes(StandardCharsets.UTF_8),
                "MATCH".getBytes(StandardCharsets.UTF_8),
                (prefix + "*").getBytes(StandardCharsets.UTF_8),
                "COUNT".getBytes(StandardCharsets.UTF_8),
                Integer.toString(Math.max(DEFAULT_LIMIT, SCAN_BATCH_SIZE)).getBytes(StandardCharsets.UTF_8)));
        return toScanChunk(rawResult);
    }

    private ScanChunk toScanChunk(Object rawResult) {
        if (!(rawResult instanceof List)) {
            return new ScanChunk("0", new ArrayList<String>());
        }
        List<?> parts = (List<?>) rawResult;
        if (parts.size() < 2) {
            return new ScanChunk("0", new ArrayList<String>());
        }
        String nextCursor = readString(parts.get(0));
        List<String> keys = new ArrayList<String>();
        Object rawKeys = parts.get(1);
        if (rawKeys instanceof List) {
            for (Object rawKey : (List<?>) rawKeys) {
                String key = readString(rawKey);
                if (key != null && !key.isEmpty()) {
                    keys.add(key);
                }
            }
        }
        return new ScanChunk(nextCursor == null || nextCursor.isEmpty() ? "0" : nextCursor, keys);
    }

    private String readString(Object rawValue) {
        if (rawValue instanceof byte[]) {
            return new String((byte[]) rawValue, StandardCharsets.UTF_8);
        }
        return rawValue == null ? null : rawValue.toString();
    }

    private void requireValidValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("cache value is required");
        }
    }

    private long requireValidTtl(Long ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds.longValue() <= 0L) {
            throw new IllegalArgumentException("ttlSeconds must be greater than 0");
        }
        return ttlSeconds.longValue();
    }

    private static final class ScanChunk {
        private final String nextCursor;
        private final List<String> keys;

        private ScanChunk(String nextCursor, List<String> keys) {
            this.nextCursor = nextCursor;
            this.keys = keys;
        }

        private String getNextCursor() {
            return nextCursor;
        }

        private List<String> getKeys() {
            return keys;
        }
    }
}
