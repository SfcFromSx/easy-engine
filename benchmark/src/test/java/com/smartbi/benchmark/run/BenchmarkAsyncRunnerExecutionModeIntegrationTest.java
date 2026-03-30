package com.smartbi.benchmark.run;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.benchmark.BenchmarkApplication;
import com.smartbi.benchmark.domain.BenchmarkDataSource;
import com.smartbi.benchmark.domain.BenchmarkJob;
import com.smartbi.benchmark.domain.BenchmarkRun;
import com.smartbi.benchmark.domain.BenchmarkStrategy;
import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import com.smartbi.benchmark.domain.RunStatus;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.repo.BenchmarkRunRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import com.smartbi.benchmark.report.BenchmarkRunReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = BenchmarkApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:benchmarkrunnermeta;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        }
)
class BenchmarkAsyncRunnerExecutionModeIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TARGET_JDBC_URL =
            "jdbc:h2:mem:benchmarkrunnerexec;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    @Autowired
    private BenchmarkJobRepository jobRepository;

    @Autowired
    private BenchmarkRunRepository runRepository;

    @Autowired
    private SqlTemplateRepository templateRepository;

    @Autowired
    private BenchmarkTestSetItemRepository testSetItemRepository;

    @Autowired
    private BenchmarkTestSetRepository testSetRepository;

    @Autowired
    private BenchmarkRunReportService reportService;

    @Autowired
    private BenchmarkDataSourceRepository dataSourceRepository;

    private BenchmarkAsyncRunner runner;

    @BeforeEach
    void setUp() throws Exception {
        runRepository.deleteAll();
        jobRepository.deleteAll();
        testSetItemRepository.deleteAll();
        testSetRepository.deleteAll();
        templateRepository.deleteAll();
        dataSourceRepository.deleteAll();

        runner = new BenchmarkAsyncRunner(
                jobRepository,
                runRepository,
                templateRepository,
                testSetItemRepository,
                testSetRepository,
                reportService,
                dataSourceRepository
        );

        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(TARGET_JDBC_URL, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS SALES");
            statement.execute("CREATE TABLE SALES (ID INT PRIMARY KEY, NAME VARCHAR(32))");
            statement.execute("INSERT INTO SALES (ID, NAME) VALUES (1, 'alpha'), (2, 'beta')");
        }
    }

    @Test
    void shouldPersistMixedExecutionModeSummaryFromTestSetSources() throws Exception {
        BenchmarkDataSource dataSource = new BenchmarkDataSource();
        dataSource.setName("runner-ds");
        dataSource.setDriverClass("org.h2.Driver");
        dataSource.setJdbcUrl(TARGET_JDBC_URL);
        dataSource.setJdbcUser("sa");
        dataSource.setJdbcPassword("");
        dataSource = dataSourceRepository.save(dataSource);

        BenchmarkTestSet testSet = new BenchmarkTestSet();
        testSet.setName("mixed-modes");
        testSet = testSetRepository.save(testSet);

        BenchmarkTestSetItem statementItem = new BenchmarkTestSetItem();
        statementItem.setTestSetId(testSet.getId());
        statementItem.setSortOrder(1);
        statementItem.setLabel("stmt");
        statementItem.setSqlText("SELECT NAME FROM SALES ORDER BY ID");
        statementItem.setExecutionMode("STATEMENT");
        testSetItemRepository.save(statementItem);

        BenchmarkTestSetItem preparedItem = new BenchmarkTestSetItem();
        preparedItem.setTestSetId(testSet.getId());
        preparedItem.setSortOrder(2);
        preparedItem.setLabel("prep");
        preparedItem.setSqlText("SELECT NAME FROM SALES WHERE ID = ?");
        preparedItem.setExecutionMode("PREPARED_STATEMENT");
        preparedItem.setParamJson("[{\"type\":\"INTEGER\",\"value\":1}]");
        testSetItemRepository.save(preparedItem);

        BenchmarkJob job = new BenchmarkJob();
        job.setName("mixed-mode-job");
        job.setDataSourceId(dataSource.getId());
        job.setConcurrentThreads(1);
        job.setRounds(2);
        job.setStrategy(BenchmarkStrategy.ROUND_ROBIN);
        job.setTestSetId(testSet.getId());
        job = jobRepository.save(job);

        BenchmarkRun run = new BenchmarkRun();
        run.setJobId(job.getId());
        run.setStatus(RunStatus.RUNNING);
        run = runRepository.save(run);

        runner.executeRun(run.getId());

        BenchmarkRun persisted = runRepository.findById(run.getId())
                .orElseThrow(() -> new AssertionError("Benchmark run not persisted"));
        assertEquals(RunStatus.COMPLETED, persisted.getStatus());
        assertEquals(Long.valueOf(2L), persisted.getTotalQueries());
        assertEquals(Long.valueOf(2L), persisted.getSuccessCount());
        assertEquals(Long.valueOf(0L), persisted.getErrorCount());

        JsonNode snapshot = JSON.readTree(persisted.getJobSnapshotJson());
        JsonNode evaluation = JSON.readTree(persisted.getEvaluationJson());

        JsonNode snapshotSummary = snapshot.path("executionModeSummary");
        assertTrue(snapshotSummary.path("mixed").asBoolean());
        assertEquals(2, snapshotSummary.path("values").size());
        assertEquals(1, snapshotSummary.path("sourceCountByMode").path("STATEMENT").asInt());
        assertEquals(1, snapshotSummary.path("sourceCountByMode").path("PREPARED_STATEMENT").asInt());
        assertNull(snapshotSummary.path("primary").textValue());

        JsonNode evaluationSummary = evaluation.path("meta").path("executionModeSummary");
        assertTrue(evaluationSummary.path("mixed").asBoolean());
        assertEquals(1, evaluationSummary.path("sourceCountByMode").path("STATEMENT").asInt());
        assertEquals(1, evaluationSummary.path("sourceCountByMode").path("PREPARED_STATEMENT").asInt());
        assertFalse(evaluation.path("issues").elements().hasNext());
    }
}
