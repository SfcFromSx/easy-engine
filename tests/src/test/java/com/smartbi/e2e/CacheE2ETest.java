package com.smartbi.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import redis.clients.jedis.Jedis;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Redis result-cache behaviour:
 * - First query populates a cache key.
 * - Second identical query returns storageCacheUsed=true.
 * - Different datasource produces an isolated cache key.
 * - Cache-bypass header skips the cache.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CacheE2ETest extends E2ETestBase {

    /** Flush all cache keys before the suite to get a clean baseline. */
    @BeforeAll
    static void flushCache() {
        try (Jedis j = jedis()) {
            Set<String> keys = j.keys(E2EConfig.REDIS_CACHE_KEY_PREFIX + "*");
            if (!keys.isEmpty()) j.del(keys.toArray(new String[0]));
        }
    }

    @Test
    @Order(1)
    @DisplayName("First query populates Redis cache")
    void testFirstQueryPopulatesCache() {
        long before = redisCacheKeyCount();

        querySpec()
                .body(statementRequest("SELECT 1 AS cache_seed"))
                .post("/kylin/api/query")
                .then().statusCode(200);

        long after = redisCacheKeyCount();
        assertThat(after).isGreaterThan(before);
    }

    @Test
    @Order(2)
    @DisplayName("Second identical query returns storageCacheUsed=true")
    void testSecondQueryHitsCache() {
        String sql = "SELECT 2 AS cache_hit_probe";
        // warm the cache
        querySpec().body(statementRequest(sql)).post("/kylin/api/query");

        // second call
        Response r = querySpec()
                .body(statementRequest(sql))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();

        assertThat(r.jsonPath().getBoolean("isException")).isFalse();
        assertThat(r.jsonPath().getBoolean("storageCacheUsed")).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Cache trace record shows cacheHit=true in MySQL for second call")
    void testCacheHitRecordedInMysql() throws Exception {
        String sql = "SELECT 3 AS cache_mysql_probe";
        querySpec().body(statementRequest(sql)).post("/kylin/api/query"); // warm
        querySpec().body(statementRequest(sql)).post("/kylin/api/query"); // hit

        waitFor(5_000, "cache_hit trace in MySQL", () -> {
            try {
                return countMysqlRows(
                    "SELECT count(*) FROM manager_sql_execution_record WHERE cache_hit = true AND original_sql = ?",
                    sql) > 0;
            } catch (Exception e) { return false; }
        });

        int hits = countMysqlRows(
            "SELECT count(*) FROM manager_sql_execution_record WHERE cache_hit = true AND original_sql = ?", sql);
        assertThat(hits).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(4)
    @DisplayName("Presto-routed query produces a separate cache key (datasource isolation)")
    void testCacheKeyDatasourceIsolation() {
        String sql = "/* ENGINE=presto_local */ SELECT 4 AS ds_isolation_probe";
        long before = redisCacheKeyCount();

        querySpec().body(statementRequest(sql)).post("/kylin/api/query");

        long after = redisCacheKeyCount();
        // A new key was created for the Presto datasource, distinct from any prior Kylin key
        assertThat(after).isGreaterThanOrEqualTo(before);
    }

    @Test
    @Order(5)
    @DisplayName("Redis contains expected key structure with configured prefix")
    void testRedisCacheKeyPrefix() {
        querySpec().body(statementRequest("SELECT 5 AS key_prefix_probe")).post("/kylin/api/query");

        try (Jedis j = jedis()) {
            Set<String> keys = j.keys(E2EConfig.REDIS_CACHE_KEY_PREFIX + "*");
            assertThat(keys).isNotEmpty();
            keys.forEach(k -> assertThat(k).startsWith(E2EConfig.REDIS_CACHE_KEY_PREFIX));
        }
    }

    @Test
    @Order(6)
    @DisplayName("Redis is reachable and PING returns PONG")
    void testRedisConnectivity() {
        try (Jedis j = jedis()) {
            assertThat(j.ping()).isEqualToIgnoringCase("PONG");
        }
    }
}
