package com.smartbi.query.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.analyze.sql.ParsedSql;
import com.smartbi.query.api.dto.SqlResponseStubDto;
import com.smartbi.query.cache.CachePolicy;
import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.integration.QueryCacheStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.Executor;

@Service
public class QueryCacheService {

    private static final Logger log = LoggerFactory.getLogger(QueryCacheService.class);

    private final QueryCacheStore cacheStore;
    private final QueryProperties queryProperties;
    private final CachePolicy cachePolicy;
    private final ObjectMapper objectMapper;
    private final Executor cacheWriteExecutor;

    @Autowired
    public QueryCacheService(QueryCacheStore cacheStore,
                             QueryProperties queryProperties,
                             ObjectMapper objectMapper,
                             @Qualifier("queryCacheWriteExecutor") Executor cacheWriteExecutor) {
        this.cacheStore = cacheStore;
        this.queryProperties = queryProperties;
        this.objectMapper = objectMapper;
        this.cacheWriteExecutor = cacheWriteExecutor;
        this.cachePolicy = new CachePolicy(queryProperties.getCache());
    }

    QueryCacheService(QueryCacheStore cacheStore,
                      QueryProperties queryProperties,
                      ObjectMapper objectMapper) {
        this(cacheStore, queryProperties, objectMapper, Runnable::run);
    }

    public CachePolicy getCachePolicy() {
        return cachePolicy;
    }

    public SqlResponseStubDto tryGet(ParsedSql parsed, String paramFingerprint, String datasourceName) {
        return tryGet(buildKey(parsed, paramFingerprint, datasourceName));
    }

    public SqlResponseStubDto tryGet(String key) {
        String value = cacheStore.get(key);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, SqlResponseStubDto.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public void put(ParsedSql parsed,
                    String paramFingerprint,
                    String datasourceName,
                    SqlResponseStubDto response) {
        put(buildKey(parsed, paramFingerprint, datasourceName), parsed, response);
    }

    public void put(String key,
                    ParsedSql parsed,
                    SqlResponseStubDto response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            if (json.getBytes(StandardCharsets.UTF_8).length > queryProperties.getCache().getMaxCacheSizeBytes()) {
                return;
            }
            final int ttl = effectiveTtl(parsed);
            cacheWriteExecutor.execute(() -> cacheStore.set(key, json, ttl));
        } catch (Exception ex) {
            log.warn("Skipping async cache write for key {}: {}", key, ex.getMessage());
        }
    }

    public int effectiveTtl(ParsedSql parsed) {
        if (parsed != null && parsed.metadata != null && parsed.metadata.cacheTtl != null && parsed.metadata.cacheTtl > 0) {
            return parsed.metadata.cacheTtl;
        }
        return queryProperties.getCache().getDefaultTtlSeconds();
    }

    public String buildKey(ParsedSql parsed, String paramFingerprint, String datasourceName) {
        String effectiveDsName = queryProperties.getCache().isDatasourceIsolationEnabled() ? datasourceName : "shared";
        if (parsed != null && parsed.metadata != null && parsed.metadata.cacheKey != null) {
            return queryProperties.getCache().getKeyPrefix() + effectiveDsName + ":" + parsed.metadata.cacheKey;
        }
        String cleanSql = parsed == null ? "" : parsed.cleanSql;
        String raw = effectiveDsName
                + "|" + cleanSql
                + "|" + (paramFingerprint == null ? "" : paramFingerprint)
                + "|" + cachePolicy.cacheModeKeyTag();
        return queryProperties.getCache().getKeyPrefix() + md5Hex(raw);
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b & 0xff));
            }
            return builder.toString();
        } catch (Exception ex) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
