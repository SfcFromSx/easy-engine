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
import com.smartbi.benchmark.jdbc.JdbcDriverRegistry;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.repo.BenchmarkRunRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import com.smartbi.benchmark.report.BenchmarkRunReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BenchmarkApplication.class)
@AutoConfigureMockMvc
class BenchmarkAsyncRunnerExecutionModeIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

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

    @Autowired
    private JdbcDriverRegistry driverRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Value("${benchmark.test.async-runner.target.driver-class}")
    private String targetDriverClass;

    @Value("${benchmark.test.async-runner.target.jdbc-url}")
    private String targetJdbcUrl;

    @Value("${benchmark.test.async-runner.target.jdbc-user}")
    private String targetJdbcUser;

    @Value("${benchmark.test.async-runner.target.jdbc-password:}")
    private String targetJdbcPassword;

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
                dataSourceRepository,
                driverRegistry
        );

        Class.forName(targetDriverClass);
        try (Connection connection = DriverManager.getConnection(targetJdbcUrl, targetJdbcUser, targetJdbcPassword);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS SALES");
            statement.execute("CREATE TABLE SALES (ID INT PRIMARY KEY, NAME VARCHAR(32))");
            statement.execute("INSERT INTO SALES (ID, NAME) VALUES (1, 'alpha'), (2, 'beta')");
        }
    }

    // Covers BenchmarkAsyncRunner#executeRun mixed execution-mode summaries and persisted run artifacts.
    @Test
    void shouldPersistMixedExecutionModeSummaryFromTestSetSources() throws Exception {
        BenchmarkDataSource dataSource = new BenchmarkDataSource();
        dataSource.setName("runner-ds");
        dataSource.setDriverClass(targetDriverClass);
        dataSource.setJdbcUrl(targetJdbcUrl);
        dataSource.setJdbcUser(targetJdbcUser);
        dataSource.setJdbcPassword(targetJdbcPassword);
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
        assertEquals("test_set", snapshot.path("sqlSourceKind").asText());
        assertEquals(2, snapshot.path("sqlSourceCount").asInt());
        assertEquals("mixed-modes", snapshot.path("testSetName").asText());
        assertEquals(2, snapshotSummary.path("values").size());
        assertEquals(1, snapshotSummary.path("sourceCountByMode").path("STATEMENT").asInt());
        assertEquals(1, snapshotSummary.path("sourceCountByMode").path("PREPARED_STATEMENT").asInt());
        assertNull(snapshotSummary.path("primary").textValue());

        assertEquals("PASS", evaluation.path("verdict").asText());
        assertEquals("COMPLETED", evaluation.path("meta").path("status").asText());
        assertEquals(2, evaluation.path("meta").path("sqlSourceCount").asInt());
        assertEquals(2, evaluation.path("metrics").path("totalQueries").asInt());
        assertEquals(2, evaluation.path("metrics").path("successCount").asInt());
        assertEquals(0, evaluation.path("metrics").path("errorCount").asInt());
        assertTrue(evaluation.path("metrics").path("latencyMs").path("p50").isNumber());
        assertTrue(evaluation.path("metrics").path("latencyMs").path("p95").isNumber());
        assertTrue(evaluation.path("metrics").has("qpsSuccessful"));
        JsonNode evaluationSummary = evaluation.path("meta").path("executionModeSummary");
        assertTrue(evaluationSummary.path("mixed").asBoolean());
        assertEquals(1, evaluationSummary.path("sourceCountByMode").path("STATEMENT").asInt());
        assertEquals(1, evaluationSummary.path("sourceCountByMode").path("PREPARED_STATEMENT").asInt());
        assertFalse(evaluation.path("issues").elements().hasNext());

        JsonNode context = loadContext(run.getId());
        assertRunContextMatchesPersistedRun(context, persisted);
        assertEquals(0, context.path("failureBreakdown").path("totalFailures").asInt());
        assertEquals(0, context.path("failureBreakdown").path("groupCount").asInt());
        assertTrue(context.path("failureBreakdown").path("groups").isArray());
        assertEquals(0, context.path("failureBreakdown").path("groups").size());
        assertFalse(context.path("comparisonDelta").path("available").asBoolean());
        assertTrue(context.path("comparisonDelta").path("reason").asText().contains("无上一次已完成同任务 Run"));
    }

    // Covers BenchmarkAsyncRunner#executeRun grouped failure diagnostics for routed and prepared failures.
    @Test
    void shouldRetainSeparateFailureGroupsForRoutedAndPreparedFailures() throws Exception {
        BenchmarkDataSource dataSource = new BenchmarkDataSource();
        dataSource.setName("runner-ds");
        dataSource.setDriverClass(targetDriverClass);
        dataSource.setJdbcUrl(targetJdbcUrl);
        dataSource.setJdbcUser(targetJdbcUser);
        dataSource.setJdbcPassword(targetJdbcPassword);
        dataSource = dataSourceRepository.save(dataSource);

        BenchmarkTestSet testSet = new BenchmarkTestSet();
        testSet.setName("failure-groups");
        testSet = testSetRepository.save(testSet);

        BenchmarkTestSetItem routedStatement = new BenchmarkTestSetItem();
        routedStatement.setTestSetId(testSet.getId());
        routedStatement.setSortOrder(1);
        routedStatement.setLabel("presto_stmt_fail");
        routedStatement.setSqlText("/* ENGINE=presto_local */ SELECT NAME FROM MISSING_PRESTO");
        routedStatement.setExecutionMode("STATEMENT");
        testSetItemRepository.save(routedStatement);

        BenchmarkTestSetItem preparedFailure = new BenchmarkTestSetItem();
        preparedFailure.setTestSetId(testSet.getId());
        preparedFailure.setSortOrder(2);
        preparedFailure.setLabel("prepared_fail");
        preparedFailure.setSqlText("SELECT NAME FROM MISSING_PREPARED WHERE ID = ?");
        preparedFailure.setExecutionMode("PREPARED_STATEMENT");
        preparedFailure.setParamJson("[{\"type\":\"INTEGER\",\"value\":1}]");
        testSetItemRepository.save(preparedFailure);

        BenchmarkJob job = new BenchmarkJob();
        job.setName("failure-group-job");
        job.setDataSourceId(dataSource.getId());
        job.setConcurrentThreads(1);
        job.setRounds(4);
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
        assertEquals(RunStatus.FAILED, persisted.getStatus());
        assertEquals(Long.valueOf(4L), persisted.getTotalQueries());
        assertEquals(Long.valueOf(0L), persisted.getSuccessCount());
        assertEquals(Long.valueOf(4L), persisted.getErrorCount());
        assertNotNull(persisted.getJobSnapshotJson());
        assertNotNull(persisted.getEvaluationJson());
        assertTrue(persisted.getErrorSample().contains("MISSING_PRESTO"));

        JsonNode snapshot = JSON.readTree(persisted.getJobSnapshotJson());
        JsonNode evaluation = JSON.readTree(persisted.getEvaluationJson());
        assertTrue(snapshot.path("executionModeSummary").path("mixed").asBoolean());
        assertEquals(2, snapshot.path("sqlSourceCount").asInt());
        assertEquals("failure-groups", snapshot.path("testSetName").asText());
        assertEquals("FAIL", evaluation.path("verdict").asText());
        assertEquals("FAILED", evaluation.path("meta").path("status").asText());
        assertEquals(2, evaluation.path("meta").path("sqlSourceCount").asInt());
        assertEquals(4, evaluation.path("metrics").path("totalQueries").asInt());
        assertEquals(0, evaluation.path("metrics").path("successCount").asInt());
        assertEquals(4, evaluation.path("metrics").path("errorCount").asInt());
        JsonNode breakdown = evaluation.path("diagnostics").path("failureBreakdown");
        assertEquals(4, breakdown.path("totalFailures").asInt());
        assertEquals(2, breakdown.path("groupCount").asInt());

        JsonNode prestoGroup = findGroup(breakdown.path("groups"), "presto_stmt_fail");
        JsonNode preparedGroup = findGroup(breakdown.path("groups"), "prepared_fail");
        assertEquals("STATEMENT", prestoGroup.path("executionMode").asText());
        assertEquals("presto_local", prestoGroup.path("routedTarget").asText());
        assertEquals(2, prestoGroup.path("failureCount").asInt());
        assertTrue(prestoGroup.path("sampleMessage").asText().contains("MISSING_PRESTO"));

        assertEquals("PREPARED_STATEMENT", preparedGroup.path("executionMode").asText());
        assertEquals("default", preparedGroup.path("routedTarget").asText());
        assertEquals(2, preparedGroup.path("failureCount").asInt());
        assertTrue(preparedGroup.path("sampleMessage").asText().contains("MISSING_PREPARED"));
        assertTrue(evaluation.path("summary").asText().contains("失败诊断"));
        assertEquals(2, evaluation.path("issues").size());

        JsonNode context = loadContext(run.getId());
        assertRunContextMatchesPersistedRun(context, persisted);
        assertEquals(breakdown, context.path("failureBreakdown"));
        assertEquals(2, context.path("failureBreakdown").path("groups").size());
        assertEquals("presto_local",
                findGroup(context.path("failureBreakdown").path("groups"), "presto_stmt_fail").path("routedTarget").asText());
        assertEquals("PREPARED_STATEMENT",
                findGroup(context.path("failureBreakdown").path("groups"), "prepared_fail").path("executionMode").asText());
        assertFalse(context.path("comparisonDelta").path("available").asBoolean());
    }

    private JsonNode loadContext(Long runId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/runs/{id}/context", runId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JSON.readTree(response);
    }

    private static void assertRunContextMatchesPersistedRun(JsonNode context, BenchmarkRun persisted) throws Exception {
        JsonNode runNode = context.path("run");
        assertEquals(persisted.getId().longValue(), runNode.path("id").asLong());
        assertEquals(persisted.getStatus().name(), runNode.path("status").asText());
        assertEquals(persisted.getTotalQueries().longValue(), runNode.path("totalQueries").asLong());
        assertEquals(persisted.getSuccessCount().longValue(), runNode.path("successCount").asLong());
        assertEquals(persisted.getErrorCount().longValue(), runNode.path("errorCount").asLong());
        assertEquals(JSON.readTree(persisted.getJobSnapshotJson()), JSON.readTree(runNode.path("jobSnapshotJson").asText()));
        assertEquals(JSON.readTree(persisted.getEvaluationJson()), JSON.readTree(runNode.path("evaluationJson").asText()));
        assertTrue(context.has("failureBreakdown"));
        assertTrue(context.has("comparisonDelta"));
    }

    private static JsonNode findGroup(JsonNode groups, String label) {
        for (JsonNode group : groups) {
            if (label.equals(group.path("sqlLabel").asText())) {
                return group;
            }
        }
        throw new AssertionError("Failure group missing: " + label);
    }
}
