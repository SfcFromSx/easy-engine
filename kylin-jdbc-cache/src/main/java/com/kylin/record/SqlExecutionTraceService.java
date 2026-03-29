package com.kylin.record;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kylin.DriverConfig;
import com.kylin.cache.RedisCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一的 SQL 执行记录入口。
 */
public class SqlExecutionTraceService {

    private static final Logger log = LoggerFactory.getLogger(SqlExecutionTraceService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 配置 Jackson 以识别 public final 字段，而无需 getter/setter 或默认构造函数
        MAPPER.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    private static final String AUDIT_LIST_KEY = "kylin_audit_trace";

    private final DriverConfig config;
    private final RedisCacheManager redisCacheManager;

    public SqlExecutionTraceService(DriverConfig config, RedisCacheManager redisCacheManager) {
        this.config = config;
        this.redisCacheManager = redisCacheManager;
    }

    public SqlExecutionTraceService(DriverConfig config) {
        this(config, null);
    }

    public void reportExecution(SqlExecutionTrace trace) {
        if (!config.isSqlTraceEnabled()) {
            return;
        }

        // 1. 标准日志打印
        log.info(
                "sql-trace dsName={} dsType={} success={} cacheHit={} durationMs={} sql={} cleanSql={} params={} error={}",
                trace.datasourceName,
                trace.datasourceType,
                trace.success,
                trace.cacheHit,
                trace.durationMs,
                limit(trace.originalSql),
                limit(trace.cleanSql),
                limit(trace.paramFingerprint),
                limit(trace.errorMessage));

        // 2. 异步上报到 Redis (由独立的 Smart-BI-Manager 后端消费)
        if (config.isSqlTraceRedisEnabled() && redisCacheManager != null) {
            try {
                String jsonStr = MAPPER.writeValueAsString(trace);
                redisCacheManager.pushToListAsync(AUDIT_LIST_KEY, jsonStr);
            } catch (JsonProcessingException e) {
                log.warn("无法序列化 SqlExecutionTrace JSON: {}", e.getMessage());
            }
        }
    }

    private String limit(String value) {
        if (value == null) return null;
        int maxLength = Math.max(64, config.getSqlTraceMaxSqlLength());
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
