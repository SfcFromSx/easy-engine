package com.smartbi.benchmark.migration;

import com.smartbi.benchmark.domain.BenchmarkDataSource;
import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import com.smartbi.benchmark.support.BenchmarkTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在独立 MySQL 容器中跑 Flyway 铺底，断言模板、任务与 prepared 语义种子完整。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class BenchmarkFlywaySeedTest {

    private static final String MYSQL_IMAGE = BenchmarkTestFixtures.get("benchmark.test.flyway-seed.mysql-image");
    private static final String MYSQL_DATABASE = BenchmarkTestFixtures.get("benchmark.test.flyway-seed.mysql-database");
    private static final String MYSQL_USERNAME = BenchmarkTestFixtures.get("benchmark.test.flyway-seed.mysql-username");
    private static final String MYSQL_PASSWORD = BenchmarkTestFixtures.get("benchmark.test.flyway-seed.mysql-password");

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName(MYSQL_DATABASE)
            .withUsername(MYSQL_USERNAME)
            .withPassword(MYSQL_PASSWORD);

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        r.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private SqlTemplateRepository sqlTemplateRepository;
    @Autowired
    private BenchmarkTestSetRepository testSetRepository;
    @Autowired
    private BenchmarkTestSetItemRepository testSetItemRepository;
    @Autowired
    private BenchmarkJobRepository jobRepository;
    @Autowired
    private BenchmarkDataSourceRepository dataSourceRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Covers Flyway benchmark seed migrations for templates, jobs, and prepared-statement fixtures.
    @Test
    void flywaySeedsTemplatesJobsAndTestSet() {
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_name) = 'benchmark_sql_template' AND LOWER(column_name) = 'execution_mode'",
                Integer.class));
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_name) = 'benchmark_test_set_item' AND LOWER(column_name) = 'sql_lib_id'",
                Integer.class));
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_name) = 'benchmark_run' AND LOWER(column_name) = 'job_snapshot_json'",
                Integer.class));
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_name) = 'benchmark_job' AND LOWER(column_name) = 'data_source_id'",
                Integer.class));
        assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_name) = 'benchmark_job' AND LOWER(column_name) = 'jdbc_url'",
                Integer.class));

        long templates = sqlTemplateRepository.count();
        assertTrue(templates >= 22, "SQL Lib starter entries should remain seeded on a fresh schema, actual=" + templates);

        List<String> templateNames = sqlTemplateRepository.findAll().stream()
                .map(template -> template.getName())
                .collect(Collectors.toList());
        assertTrue(templateNames.contains("sample_agg"), "应保留 sample_agg 示例模板供回归任务复用");

        String sampleAggSql = sqlTemplateRepository.findAll().stream()
                .filter(template -> "sample_agg".equals(template.getName()))
                .map(template -> template.getSqlText())
                .findFirst()
                .orElseThrow(() -> new AssertionError("sample_agg template missing"));
        assertEquals("SELECT sum(price) AS sum_price FROM KYLIN_SALES", sampleAggSql,
                "fresh schema should replace the original SELECT 1 placeholder with the seeded learn_kylin example");

        Optional<BenchmarkTestSet> reg = testSetRepository.findAll().stream()
                .filter(t -> "learn_kylin_regression".equals(t.getName()))
                .findFirst();
        assertTrue(reg.isPresent(), "应存在 learn_kylin_regression 测试集");

        long items = testSetItemRepository.countByTestSetId(reg.get().getId());
        assertTrue(items >= 15, "测试集条目应 >= 15, actual=" + items);

        long jobs = jobRepository.count();
        assertTrue(jobs >= 4, "任务应 >= 4（V2 默认 + V5 三条）, actual=" + jobs);

        Set<String> seededJobNames = jobRepository.findAll().stream()
                .map(job -> job.getName())
                .collect(Collectors.toSet());
        assertTrue(seededJobNames.contains("default-kylin-cached"));
        assertTrue(seededJobNames.contains("benchmark-smoke-global"));
        assertTrue(seededJobNames.contains("benchmark-testset-round-robin"));
        assertTrue(seededJobNames.contains("benchmark-cache-penetration"));

        boolean smokeSet = testSetRepository.findAll().stream()
                .anyMatch(t -> "smoke_kylin_only".equals(t.getName()));
        assertTrue(smokeSet, "V6 应存在 smoke_kylin_only 测试集");

        boolean engineQueryJob = jobRepository.findAll().stream()
                .anyMatch(j -> j.getDataSourceId() != null);
        assertTrue(engineQueryJob, "应存在具有 dataSourceId 的 benchmark job");

        boolean preparedTemplate = sqlTemplateRepository.findAll().stream()
                .anyMatch(t -> "PREPARED_STATEMENT".equalsIgnoreCase(t.getExecutionMode())
                        && t.getParamJson() != null && !t.getParamJson().trim().isEmpty());
        assertTrue(preparedTemplate, "应存在 prepared statement 模板种子");

        boolean preparedItem = testSetItemRepository.findAll().stream()
                .anyMatch(i -> "PREPARED_STATEMENT".equalsIgnoreCase(i.getExecutionMode())
                        && i.getParamJson() != null && !i.getParamJson().trim().isEmpty());
        assertTrue(preparedItem, "应存在 prepared statement 测试集条目");
    }

    // Covers Flyway benchmark datasource seed cleanup for duplicate legacy job connections.
    @Test
    void flywaySeedsOneCanonicalQueryGatewayDatasource() {
        assertEquals(1L, dataSourceRepository.count(),
                "相同 JDBC 明细的 benchmark 种子数据源应收敛为 1 条共享记录");

        BenchmarkDataSource dataSource = dataSourceRepository.findAll().get(0);
        assertEquals("engine-query-default", dataSource.getName());
        assertEquals("jdbc:kylin://127.0.0.1:8092/learn_kylin", dataSource.getJdbcUrl());
        assertEquals("org.apache.kylin.jdbc.Driver", dataSource.getDriverClass());

        Set<Long> referencedDataSourceIds = jobRepository.findAll().stream()
                .map(job -> {
                    assertNotNull(job.getDataSourceId(), "seeded benchmark jobs should keep a datasource reference");
                    return job.getDataSourceId();
                })
                .collect(Collectors.toSet());

        assertEquals(Collections.singleton(dataSource.getId()), referencedDataSourceIds,
                "all seeded benchmark jobs should point at the canonical shared query gateway datasource");
    }
}
