package com.smartbi.benchmark.web;

import com.smartbi.benchmark.BenchmarkApplication;
import com.smartbi.benchmark.domain.BenchmarkJob;
import com.smartbi.benchmark.domain.BenchmarkRun;
import com.smartbi.benchmark.domain.BenchmarkStrategy;
import com.smartbi.benchmark.domain.RunStatus;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.repo.BenchmarkRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = BenchmarkApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:runcontrollerctx;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        }
)
@AutoConfigureMockMvc
class RunControllerContextTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BenchmarkRunRepository runRepository;

    @Autowired
    private BenchmarkJobRepository jobRepository;

    @BeforeEach
    void setUp() {
        runRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void shouldExposeFailureBreakdownFromEvaluationJson() throws Exception {
        BenchmarkJob job = new BenchmarkJob();
        job.setName("ctx-job");
        job.setConcurrentThreads(1);
        job.setRounds(2);
        job.setStrategy(BenchmarkStrategy.ROUND_ROBIN);
        job = jobRepository.save(job);

        BenchmarkRun run = new BenchmarkRun();
        run.setJobId(job.getId());
        run.setStatus(RunStatus.FAILED);
        run.setTotalQueries(2L);
        run.setSuccessCount(0L);
        run.setErrorCount(2L);
        run.setErrorSample("presto route failed");
        run.setEvaluationJson("{\"diagnostics\":{\"failureBreakdown\":{\"totalFailures\":2,\"groupCount\":1,"
                + "\"groups\":[{\"groupKey\":\"presto_stmt_fail|STATEMENT|presto_local\","
                + "\"sqlLabel\":\"presto_stmt_fail\",\"executionMode\":\"STATEMENT\","
                + "\"routedTarget\":\"presto_local\",\"failureCount\":2,"
                + "\"sampleMessage\":\"presto route failed\"}]}}}");
        run = runRepository.save(run);

        mockMvc.perform(get("/api/v1/runs/{id}/context", run.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failureBreakdown.totalFailures").value(2))
                .andExpect(jsonPath("$.failureBreakdown.groupCount").value(1))
                .andExpect(jsonPath("$.failureBreakdown.groups[0].sqlLabel").value("presto_stmt_fail"))
                .andExpect(jsonPath("$.failureBreakdown.groups[0].routedTarget").value("presto_local"))
                .andExpect(jsonPath("$.run.status").value("FAILED"))
                .andExpect(jsonPath("$.run.totalQueries").value(2))
                .andExpect(jsonPath("$.run.evaluationJson").value(run.getEvaluationJson()))
                .andExpect(jsonPath("$.comparisonDelta.available").value(false));
    }

    @Test
    void shouldExposeSuccessContextShapeAndComparisonDelta() throws Exception {
        BenchmarkJob job = new BenchmarkJob();
        job.setName("ctx-success-job");
        job.setConcurrentThreads(1);
        job.setRounds(3);
        job.setStrategy(BenchmarkStrategy.ROUND_ROBIN);
        job = jobRepository.save(job);

        BenchmarkRun baseline = new BenchmarkRun();
        baseline.setJobId(job.getId());
        baseline.setStatus(RunStatus.COMPLETED);
        baseline.setTotalQueries(3L);
        baseline.setSuccessCount(3L);
        baseline.setErrorCount(0L);
        baseline.setDurationMs(15L);
        baseline.setQps(200.0);
        baseline.setP50Ms(4.0);
        baseline.setP95Ms(6.0);
        baseline.setP99Ms(7.0);
        baseline.setJobSnapshotJson("{\"jobName\":\"ctx-success-job\"}");
        baseline.setEvaluationJson("{\"diagnostics\":{\"failureBreakdown\":{\"totalFailures\":0,\"groupCount\":0,\"groups\":[]}}}");
        baseline = runRepository.save(baseline);

        BenchmarkRun current = new BenchmarkRun();
        current.setJobId(job.getId());
        current.setStatus(RunStatus.COMPLETED);
        current.setTotalQueries(3L);
        current.setSuccessCount(3L);
        current.setErrorCount(0L);
        current.setDurationMs(12L);
        current.setQps(250.0);
        current.setP50Ms(3.0);
        current.setP95Ms(5.0);
        current.setP99Ms(6.0);
        current.setJobSnapshotJson("{\"jobName\":\"ctx-success-job\",\"sqlSourceCount\":2}");
        current.setEvaluationJson("{\"verdict\":\"PASS\",\"diagnostics\":{\"failureBreakdown\":{\"totalFailures\":0,\"groupCount\":0,\"groups\":[]}}}");
        current = runRepository.save(current);

        mockMvc.perform(get("/api/v1/runs/{id}/context", current.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.id").value(current.getId()))
                .andExpect(jsonPath("$.run.status").value("COMPLETED"))
                .andExpect(jsonPath("$.run.jobSnapshotJson").value(current.getJobSnapshotJson()))
                .andExpect(jsonPath("$.run.evaluationJson").value(current.getEvaluationJson()))
                .andExpect(jsonPath("$.failureBreakdown.totalFailures").value(0))
                .andExpect(jsonPath("$.failureBreakdown.groupCount").value(0))
                .andExpect(jsonPath("$.failureBreakdown.groups").isArray())
                .andExpect(jsonPath("$.failureBreakdown.groups").isEmpty())
                .andExpect(jsonPath("$.comparisonDelta.available").value(true))
                .andExpect(jsonPath("$.comparisonDelta.baselineRunId").value(baseline.getId()))
                .andExpect(jsonPath("$.comparisonDelta.deltas.p50Ms.current").value(3.0))
                .andExpect(jsonPath("$.comparisonDelta.deltas.p50Ms.baseline").value(4.0))
                .andExpect(jsonPath("$.comparisonDelta.deltas.qpsSuccessful.current").value(250.0))
                .andExpect(jsonPath("$.comparisonDelta.deltas.qpsSuccessful.baseline").value(200.0));
    }
}
