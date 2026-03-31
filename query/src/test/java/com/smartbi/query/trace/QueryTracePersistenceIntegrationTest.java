package com.smartbi.query.trace;

import com.smartbi.query.EngineQueryApplication;
import com.smartbi.query.domain.ParseStatus;
import com.smartbi.query.domain.SqlExecutionRecord;
import com.smartbi.query.domain.SqlPatternStats;
import com.smartbi.query.integration.QueryCacheStore;
import com.smartbi.query.repo.SqlExecutionRecordRepository;
import com.smartbi.query.repo.SqlPatternStatsRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = EngineQueryApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:trace_persistence;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "engine.query.datasource.default.name=default",
                "engine.query.datasource.default.type=h2",
                "engine.query.datasource.default.driver-class=org.h2.Driver",
                "engine.query.datasource.default.jdbc-url=jdbc:h2:mem:trace_query;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "engine.query.datasource.default.username=sa",
                "engine.query.datasource.default.password=",
                "engine.query.auth.username=ADMIN",
                "engine.query.auth.password=KYLIN"
        }
)
@AutoConfigureMockMvc
@Import(QueryTracePersistenceIntegrationTest.CacheOnlyTestConfiguration.class)
class QueryTracePersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SqlExecutionRecordRepository recordRepository;

    @Autowired
    private SqlPatternStatsRepository patternStatsRepository;

    @BeforeEach
    void setUp() throws Exception {
        recordRepository.deleteAll();
        patternStatsRepository.deleteAll();

        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:trace_query;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS SALES");
            statement.execute("CREATE TABLE SALES (ID INT PRIMARY KEY, NAME VARCHAR(32))");
            statement.execute("INSERT INTO SALES (ID, NAME) VALUES (1, 'alpha'), (2, 'beta')");
        }
    }

    @Test
    void shouldPersistTraceRowsDirectlyToTraceDatabase() throws Exception {
        String body = "{\"sql\":\"SELECT NAME FROM SALES ORDER BY ID\",\"project\":\"demo\"}";

        mockMvc.perform(post("/kylin/api/query")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());

        Assertions.assertEquals(1, recordRepository.count());
        SqlExecutionRecord record = recordRepository.findAll().get(0);
        Assertions.assertEquals("default", record.getDatasourceName());
        Assertions.assertEquals("STATEMENT", record.getExecutionMode());
        Assertions.assertEquals(ParseStatus.OK, record.getParseStatus());
        Assertions.assertNotNull(record.getSqlFingerprint());
        Assertions.assertTrue(record.getRawPayload().contains("\"datasourceName\":\"default\""));

        Optional<SqlPatternStats> stats = patternStatsRepository.findBySqlFingerprint(record.getSqlFingerprint());
        Assertions.assertTrue(stats.isPresent());
        Assertions.assertEquals(1L, stats.get().getExecutionCount());
        Assertions.assertEquals("SELECT NAME FROM SALES ORDER BY ID", stats.get().getCleanSqlSample());
    }

    private static String authHeader() {
        return "Basic " + Base64.getEncoder().encodeToString("ADMIN:KYLIN".getBytes());
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
