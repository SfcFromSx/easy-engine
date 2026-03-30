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
                job, run, 1, Collections.singletonList(SqlExecutionMode.STATEMENT)));

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
}
