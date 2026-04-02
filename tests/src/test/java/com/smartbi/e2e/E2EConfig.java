package com.smartbi.e2e;

/**
 * Central configuration for all E2E tests.
 * Values default to the docker-compose local stack and can be overridden
 * via Maven -D flags or system properties.
 */
public final class E2EConfig {

    public static final String QUERY_URL    = prop("e2e.query.url",   "http://localhost:8092");
    public static final String MANAGER_URL  = prop("e2e.manager.url", "http://localhost:8090");

    // MySQL
    public static final String MYSQL_HOST     = prop("e2e.mysql.host",     "localhost");
    public static final int    MYSQL_PORT     = Integer.parseInt(prop("e2e.mysql.port", "3307"));
    public static final String MYSQL_URL      = prop("e2e.mysql.url",      "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/engine_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    public static final String MYSQL_USER     = prop("e2e.mysql.user",     "engine");
    public static final String MYSQL_PASSWORD = prop("e2e.mysql.password", "engine123");

    // Redis
    public static final String REDIS_HOST = prop("e2e.redis.host", "localhost");
    public static final int    REDIS_PORT = Integer.parseInt(prop("e2e.redis.port", "6380"));
    public static final String REDIS_CACHE_KEY_PREFIX = "kylin_cache:";

    // Kylin (direct REST — used by JDBC driver)
    public static final String KYLIN_HOST    = prop("e2e.kylin.host", "localhost");
    public static final int    KYLIN_PORT    = Integer.parseInt(prop("e2e.kylin.port", "17070"));
    public static final String KYLIN_PROJECT = "learn_kylin";
    public static final String KYLIN_USER    = "ADMIN";
    public static final String KYLIN_PASS    = "KYLIN";

    // Query JDBC (Kylin JDBC driver pointing at query service)
    public static final String QUERY_JDBC_URL = "jdbc:kylin://" + prop("e2e.query.url", "localhost:8092")
            .replace("http://", "") + "/" + KYLIN_PROJECT;

    // Presto
    public static final String PRESTO_JDBC_URL = prop("e2e.presto.url", "jdbc:presto://localhost:18081/tpch/tiny");
    public static final String PRESTO_DS_NAME  = "presto_local";

    // Trino
    public static final String TRINO_JDBC_URL = prop("e2e.trino.url", "jdbc:trino://localhost:18080/tpch/tiny");
    public static final String TRINO_DS_NAME  = "trino_local";

    // Auth header for query service (ADMIN:KYLIN base64)
    public static final String QUERY_AUTH_HEADER = "Basic QURNSU46S1lMSU4="; // ADMIN:KYLIN

    private static String prop(String key, String def) {
        String v = System.getProperty(key);
        return (v != null && !v.isEmpty()) ? v : def;
    }

    private E2EConfig() {}
}
