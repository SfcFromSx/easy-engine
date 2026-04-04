package com.smartbi.query.trace;

import com.smartbi.query.EngineQueryApplication;
import com.smartbi.query.domain.ParseStatus;
import com.smartbi.query.domain.SqlExecutionRecord;
import com.smartbi.query.domain.SqlPatternStats;
import com.smartbi.query.integration.QueryCacheStore;
import com.smartbi.query.repo.SqlExecutionRecordRepository;
import com.smartbi.query.repo.SqlPatternStatsRepository;
import com.smartbi.query.support.QuerySpringTestOverrides;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EngineQueryApplication.class)
@AutoConfigureMockMvc
@Import(QueryTracePersistenceIntegrationTest.CacheOnlyTestConfiguration.class)
class QueryTracePersistenceIntegrationTest {

    private static final String MYSQL_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String QUERY_ENDPOINT = "/kylin/api/query";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String AUTH_SEPARATOR = ":";
    private static final String TRACE_QUERY_BODY =
            "{\"sql\":\"SELECT NAME FROM SALES ORDER BY ID\",\"project\":\"demo\"}";
    private static final String SALES_DROP_SQL = "DROP TABLE IF EXISTS SALES";
    private static final String SALES_CREATE_SQL = "CREATE TABLE SALES (ID INT PRIMARY KEY, NAME VARCHAR(32))";
    private static final String SALES_INSERT_SQL =
            "INSERT INTO SALES (ID, NAME) VALUES (1, 'alpha'), (2, 'beta')";
    private static final String DEFAULT_DATASOURCE_NAME = "default";
    private static final String STATEMENT_EXECUTION_MODE = "STATEMENT";
    private static final String RAW_PAYLOAD_DATASOURCE_FRAGMENT = "\"datasourceName\":\"default\"";
    private static final String CLEAN_SQL_SAMPLE = "SELECT NAME FROM SALES ORDER BY ID";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SqlExecutionRecordRepository recordRepository;

    @Autowired
    private SqlPatternStatsRepository patternStatsRepository;

    @Value("${engine.query.datasource.default.jdbc-url}")
    private String defaultJdbcUrl;

    @Value("${engine.query.datasource.default.username}")
    private String defaultJdbcUser;

    @Value("${engine.query.datasource.default.password:}")
    private String defaultJdbcPassword;

    @Value("${engine.query.auth.username}")
    private String authUsername;

    @Value("${engine.query.auth.password}")
    private String authPassword;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        QuerySpringTestOverrides.register(registry);
    }

    @BeforeEach
    void setUp() throws Exception {
        recordRepository.deleteAll();
        patternStatsRepository.deleteAll();

        Class.forName(MYSQL_DRIVER_CLASS);
        try (Connection connection = DriverManager.getConnection(defaultJdbcUrl, defaultJdbcUser, defaultJdbcPassword);
             Statement statement = connection.createStatement()) {
            statement.execute(SALES_DROP_SQL);
            statement.execute(SALES_CREATE_SQL);
            statement.execute(SALES_INSERT_SQL);
        }
    }

    // Covers JdbcTraceWriter#publish and JdbcTraceWriter#upsertPatternStats through the persisted HTTP trace path.
    @Test
    void shouldPersistTraceRowsDirectlyToTraceDatabase() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRACE_QUERY_BODY))
                .andExpect(status().isOk());

        Assertions.assertEquals(1, recordRepository.count());
        SqlExecutionRecord record = recordRepository.findAll().get(0);
        Assertions.assertEquals(DEFAULT_DATASOURCE_NAME, record.getDatasourceName());
        Assertions.assertEquals(STATEMENT_EXECUTION_MODE, record.getExecutionMode());
        Assertions.assertEquals(ParseStatus.OK, record.getParseStatus());
        Assertions.assertNotNull(record.getCacheKey());
        Assertions.assertTrue(record.getCacheKey().startsWith("kylin_cache:"));
        Assertions.assertNotNull(record.getSqlFingerprint());
        Assertions.assertTrue(record.getRawPayload().contains(RAW_PAYLOAD_DATASOURCE_FRAGMENT));

        Optional<SqlPatternStats> stats = patternStatsRepository.findBySqlFingerprint(record.getSqlFingerprint());
        Assertions.assertTrue(stats.isPresent());
        Assertions.assertEquals(1L, stats.get().getExecutionCount());
        Assertions.assertEquals(CLEAN_SQL_SAMPLE, stats.get().getCleanSqlSample());
    }

    private String authHeader() {
        String credentials = authUsername + AUTH_SEPARATOR + authPassword;
        return BASIC_PREFIX + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class CacheOnlyTestConfiguration {

        @Bean
        @Primary
        public QueryCacheStore queryCacheStore() {
            return new QueryCacheStore() {
                private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<String, String>();

                @Override
                public String get(String key) {
                    return cache.get(key);
                }

                @Override
                public void set(String key, String value, int ttlSeconds) {
                    cache.put(key, value);
                }
            };
        }
    }
}
