package com.smartbi.benchmark.run;

import com.smartbi.benchmark.domain.BenchmarkJob;
import com.smartbi.benchmark.domain.BenchmarkRun;
import com.smartbi.benchmark.domain.RunStatus;
import com.smartbi.benchmark.domain.SqlTemplate;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.repo.BenchmarkRunRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class BenchmarkExecutionService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkExecutionService.class);
    private static final Duration STALE_RUN_TIMEOUT = Duration.ofMinutes(15);

    private final BenchmarkJobRepository jobRepository;
    private final BenchmarkRunRepository runRepository;
    private final SqlTemplateRepository templateRepository;
    private final BenchmarkTestSetItemRepository testSetItemRepository;
    private final BenchmarkAsyncRunner asyncRunner;

    public BenchmarkExecutionService(BenchmarkJobRepository jobRepository,
                                     BenchmarkRunRepository runRepository,
                                     SqlTemplateRepository templateRepository,
                                     BenchmarkTestSetItemRepository testSetItemRepository,
                                     BenchmarkAsyncRunner asyncRunner) {
        this.jobRepository = jobRepository;
        this.runRepository = runRepository;
        this.templateRepository = templateRepository;
        this.testSetItemRepository = testSetItemRepository;
        this.asyncRunner = asyncRunner;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcileStaleRunsOnStartup() {
        int recovered = reconcileStaleRuns();
        if (recovered > 0) {
            log.warn("Recovered {} stale benchmark run(s) left in RUNNING state", recovered);
        }
    }

    @Transactional
    public BenchmarkRun startRun(long jobId) {
        reconcileStaleRuns();
        BenchmarkJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("job not found"));
        if (job.getTestSetId() != null) {
            long n = testSetItemRepository.countByTestSetId(job.getTestSetId());
            if (n == 0) {
                throw new IllegalStateException("所选测试集为空或不存在，请先从 SQL Lib 选择 SQL");
            }
        } else {
            List<SqlTemplate> templates = templateRepository.findAllByOrderByIdAsc();
            if (templates.isEmpty()) {
                throw new IllegalStateException("未绑定测试集且 SQL Lib 为空，请先维护 SQL Lib 或关联测试集");
            }
        }
        BenchmarkRun run = new BenchmarkRun();
        run.setJobId(jobId);
        run.setStatus(RunStatus.RUNNING);
        run = runRepository.save(run);
        triggerAsyncRunAfterCommit(run.getId());
        return run;
    }

    @Transactional
    public int reconcileStaleRuns() {
        Instant cutoff = Instant.now().minus(STALE_RUN_TIMEOUT);
        List<BenchmarkRun> staleRuns = new ArrayList<BenchmarkRun>();
        for (BenchmarkRun run : runRepository.findByStatus(RunStatus.RUNNING)) {
            if (run.getStartedAt() != null && run.getStartedAt().isBefore(cutoff)) {
                staleRuns.add(run);
            }
        }
        for (BenchmarkRun staleRun : staleRuns) {
            staleRun.setStatus(RunStatus.FAILED);
            staleRun.setEndedAt(Instant.now());
            if (!StringUtils.hasText(staleRun.getErrorSample())) {
                staleRun.setErrorSample("Recovered stale RUNNING run after benchmark service restart.");
            }
            runRepository.save(staleRun);
        }
        return staleRuns.size();
    }

    private void triggerAsyncRunAfterCommit(Long runId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            asyncRunner.executeRun(runId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new AfterCommitRunSynchronization(asyncRunner, runId));
    }

    private static final class AfterCommitRunSynchronization implements TransactionSynchronization {

        private final BenchmarkAsyncRunner asyncRunner;
        private final Long runId;

        private AfterCommitRunSynchronization(BenchmarkAsyncRunner asyncRunner, Long runId) {
            this.asyncRunner = asyncRunner;
            this.runId = runId;
        }

        @Override
        public void afterCommit() {
            asyncRunner.executeRun(runId);
        }
    }
}
