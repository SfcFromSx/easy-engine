package com.smartbi.engine.web;

import com.smartbi.engine.web.dto.CacheInfoDto;
import com.smartbi.engine.web.dto.CacheKeyDetailDto;
import com.smartbi.engine.web.dto.CacheKeyDto;
import com.smartbi.engine.web.dto.CacheKeyUpsertRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cache")
public class CacheManagementController {

    private final StringRedisTemplate redisTemplate;
    private static final String CACHE_KEY_PREFIX = "kylin_cache:";
    private static final String CACHE_KEY_PATTERN = "kylin_cache:*";
    private static final int SCAN_BATCH_SIZE = 100;

    public CacheManagementController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/info")
    public CacheInfoDto getCacheInfo() {
        CacheInfoDto info = new CacheInfoDto();
        long totalSize = 0;
        int keyCount = 0;

        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(CACHE_KEY_PATTERN)
                        .count(SCAN_BATCH_SIZE)
                        .build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                keyCount++;
                Long size = redisTemplate.opsForValue().size(key);
                if (size != null) {
                    totalSize += size;
                }
            }
        }

        info.setTotalSizeBytes(totalSize);
        info.setKeyCount(keyCount);
        return info;
    }

    @GetMapping("/keys")
    public List<CacheKeyDto> listCacheKeys(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int limit) {
        List<CacheKeyDto> keys = new ArrayList<>();
        int skipped = 0;
        int collected = 0;

        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(CACHE_KEY_PATTERN)
                        .count(SCAN_BATCH_SIZE)
                        .build())) {
            while (cursor.hasNext() && collected < limit) {
                String key = cursor.next();
                if (skipped < offset) {
                    skipped++;
                    continue;
                }
                CacheKeyDto dto = new CacheKeyDto();
                dto.setKey(key);
                Long size = redisTemplate.opsForValue().size(key);
                dto.setSizeBytes(size != null ? size : 0L);
                Long ttl = redisTemplate.getExpire(key);
                dto.setTtlSeconds(ttl != null ? ttl : -1L);
                keys.add(dto);
                collected++;
            }
        }

        return keys;
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
}
