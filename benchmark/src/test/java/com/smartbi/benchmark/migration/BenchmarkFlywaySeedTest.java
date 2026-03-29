package com.smartbi.benchmark.migration;

import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在独立 PostgreSQL 容器中跑 Flyway 铺底，断言模板、任务与 prepared 语义种子完整。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class BenchmarkFlywaySeedTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("engine_db")
            .withUsername("engine")
            .withPassword("engine123");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private SqlTemplateRepository sqlTemplateRepository;
    @Autowired
    private BenchmarkTestSetRepository testSetRepository;
    @Autowired
    private BenchmarkTestSetItemRepository testSetItemRepository;
    @Autowired
    private BenchmarkJobRepository jobRepository;

    @Test
    void flywaySeedsTemplatesJobsAndTestSet() {
        long templates = sqlTemplateRepository.count();
        assertTrue(templates >= 22, "全局 SQL 模板应 >= 22（V2 两条 + V5 约 20 条）, actual=" + templates);

        Optional<BenchmarkTestSet> reg = testSetRepository.findAll().stream()
                .filter(t -> "learn_kylin_regression".equals(t.getName()))
                .findFirst();
        assertTrue(reg.isPresent(), "应存在 learn_kylin_regression 测试集");

        long items = testSetItemRepository.countByTestSetId(reg.get().getId());
        assertTrue(items >= 15, "测试集条目应 >= 15, actual=" + items);

        long jobs = jobRepository.count();
        assertTrue(jobs >= 4, "任务应 >= 4（V2 默认 + V5 三条）, actual=" + jobs);

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
}
