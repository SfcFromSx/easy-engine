package com.smartbi.query.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.query.api.dto.SqlResponseStubDto;
import com.smartbi.query.cache.CachePolicy;
import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.integration.QueryCacheStore;
import com.smartbi.query.parsing.SqlCommentParser;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class QueryCacheService {

    private final QueryCacheStore cacheStore;
    private final QueryProperties queryProperties;
    private final CachePolicy cachePolicy;
    private final ObjectMapper objectMapper;

    public QueryCacheService(QueryCacheStore cacheStore,
                             QueryProperties queryProperties,
                             ObjectMapper objectMapper) {
        this.cacheStore = cacheStore;
        this.queryProperties = queryProperties;
        this.objectMapper = objectMapper;
        this.cachePolicy = new CachePolicy(queryProperties.getCache());
    }

    public CachePolicy getCachePolicy() {
        return cachePolicy;
    }

    public SqlResponseStubDto tryGet(SqlCommentParser.ParsedSql parsed, String paramFingerprint, String datasourceName) {
        String value = cacheStore.get(buildKey(parsed, paramFingerprint, datasourceName));
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, SqlResponseStubDto.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public void put(SqlCommentParser.ParsedSql parsed,
                    String paramFingerprint,
                    String datasourceName,
                    SqlResponseStubDto response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            if (json.getBytes(StandardCharsets.UTF_8).length > queryProperties.getCache().getMaxCacheSizeBytes()) {
                return;
            }
            cacheStore.set(buildKey(parsed, paramFingerprint, datasourceName), json, effectiveTtl(parsed));
        } catch (Exception ignored) {
        }
    }

    public int effectiveTtl(SqlCommentParser.ParsedSql parsed) {
        if (parsed != null && parsed.metadata != null && parsed.metadata.cacheTtl != null && parsed.metadata.cacheTtl > 0) {
            return parsed.metadata.cacheTtl;
        }
        return queryProperties.getCache().getDefaultTtlSeconds();
    }

    public String buildKey(SqlCommentParser.ParsedSql parsed, String paramFingerprint, String datasourceName) {
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
