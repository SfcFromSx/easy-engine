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
                "spring.datasource.url=jdbc:h2:mem:runcontrollerctx;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
                .andExpect(jsonPath("$.failureBreakdown.groups[0].routedTarget").value("presto_local"));
    }
}
