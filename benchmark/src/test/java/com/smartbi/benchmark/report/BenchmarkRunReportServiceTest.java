package com.smartbi.benchmark.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.benchmark.domain.BenchmarkJob;
import com.smartbi.benchmark.domain.BenchmarkRun;
import com.smartbi.benchmark.domain.BenchmarkStrategy;
import com.smartbi.benchmark.domain.RunStatus;
import com.smartbi.benchmark.domain.SqlExecutionMode;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkRunReportServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final BenchmarkRunReportService service =
            new BenchmarkRunReportService(Mockito.mock(BenchmarkDataSourceRepository.class));

    @Test
    void shouldIncludeSingleExecutionModeInJobSnapshotAndEvaluation() throws Exception {
        BenchmarkJob job = job("Templates");
        BenchmarkRun run = completedRun();

        JsonNode snapshot = JSON.readTree(service.buildJobSnapshotJson(
                job, 1, null, Collections.singletonList(SqlExecutionMode.STATEMENT)));
        JsonNode evaluation = JSON.readTree(service.buildCompletedEvaluation(
                job, run, 1, Collections.singletonList(SqlExecutionMode.STATEMENT),
                Collections.<Map<String, Object>>emptyList()));

        assertEquals("STATEMENT",
                snapshot.path("executionModeSummary").path("primary").asText());
        assertEquals("STATEMENT",
                evaluation.path("meta").path("executionModeSummary").path("primary").asText());
        assertTrue(snapshot.path("executionModeSummary").path("sourceCountByMode").has("STATEMENT"));
    }

    @Test
    void shouldRepresentMixedExecutionModesExplicitly() throws Exception {
        BenchmarkJob job = job("Mixed");

        JsonNode snapshot = JSON.readTree(service.buildJobSnapshotJson(
                job, 2, "set-a",
                Arrays.asList(SqlExecutionMode.STATEMENT, SqlExecutionMode.PREPARED_STATEMENT)));

        JsonNode summary = snapshot.path("executionModeSummary");
        assertTrue(summary.path("mixed").asBoolean());
        assertEquals(2, summary.path("values").size());
        assertEquals(1, summary.path("sourceCountByMode").path("STATEMENT").asInt());
        assertEquals(1, summary.path("sourceCountByMode").path("PREPARED_STATEMENT").asInt());
    }

    @Test
    void shouldIncludeGroupedFailureBreakdownInEvaluation() throws Exception {
        BenchmarkJob job = job("Failures");
        BenchmarkRun run = completedRun();
        run.setStatus(RunStatus.FAILED);
        run.setSuccessCount(0L);
        run.setErrorCount(4L);
        run.setErrorSample("presto route failed");

        JsonNode evaluation = JSON.readTree(service.buildCompletedEvaluation(
                job,
                run,
                2,
                Arrays.asList(SqlExecutionMode.STATEMENT, SqlExecutionMode.PREPARED_STATEMENT),
                Arrays.asList(
                        failureGroup("presto_stmt_fail", "STATEMENT", "presto_local", 2, "presto route failed"),
                        failureGroup("prepared_fail", "PREPARED_STATEMENT", "default", 2, "prepared failed")
                )));

        JsonNode breakdown = evaluation.path("diagnostics").path("failureBreakdown");
        assertEquals(4, breakdown.path("totalFailures").asInt());
        assertEquals(2, breakdown.path("groupCount").asInt());
        assertEquals("presto_local", findGroup(breakdown.path("groups"), "presto_stmt_fail").path("routedTarget").asText());
        assertEquals("PREPARED_STATEMENT",
                findGroup(breakdown.path("groups"), "prepared_fail").path("executionMode").asText());
        assertTrue(evaluation.path("summary").asText().contains("失败诊断"));
        assertEquals(2, evaluation.path("issues").size());
    }

    private static BenchmarkJob job(String name) {
        BenchmarkJob job = new BenchmarkJob();
        job.setId(7L);
        job.setName(name);
        job.setStrategy(BenchmarkStrategy.ROUND_ROBIN);
        job.setConcurrentThreads(1);
        job.setRounds(2);
        return job;
    }

    private static BenchmarkRun completedRun() {
        BenchmarkRun run = new BenchmarkRun();
        run.setStatus(RunStatus.COMPLETED);
        run.setTotalQueries(1L);
        run.setSuccessCount(1L);
        run.setErrorCount(0L);
        run.setDurationMs(5L);
        run.setQps(200.0);
        run.setP50Ms(5.0);
        run.setP95Ms(5.0);
        run.setP99Ms(5.0);
        return run;
    }

    private static Map<String, Object> failureGroup(String label,
                                                    String executionMode,
                                                    String routedTarget,
                                                    int failureCount,
                                                    String sampleMessage) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("groupKey", label + "|" + executionMode + "|" + routedTarget);
        group.put("sqlLabel", label);
        group.put("executionMode", executionMode);
        group.put("routedTarget", routedTarget);
        group.put("failureCount", failureCount);
        group.put("sampleMessage", sampleMessage);
        return group;
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
