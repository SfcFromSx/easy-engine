package com.smartbi.benchmark.run;

import com.smartbi.benchmark.domain.BenchmarkJob;
import com.smartbi.benchmark.domain.BenchmarkRun;
import com.smartbi.benchmark.domain.RunStatus;
import com.smartbi.benchmark.domain.SqlTemplate;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.repo.BenchmarkRunRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BenchmarkExecutionServiceTest {

    private BenchmarkJobRepository jobRepository;
    private BenchmarkRunRepository runRepository;
    private SqlTemplateRepository templateRepository;
    private BenchmarkTestSetItemRepository testSetItemRepository;
    private BenchmarkAsyncRunner asyncRunner;
    private BenchmarkExecutionService service;

    @BeforeEach
    void setUp() {
        jobRepository = mock(BenchmarkJobRepository.class);
        runRepository = mock(BenchmarkRunRepository.class);
        templateRepository = mock(SqlTemplateRepository.class);
        testSetItemRepository = mock(BenchmarkTestSetItemRepository.class);
        asyncRunner = mock(BenchmarkAsyncRunner.class);
        service = new BenchmarkExecutionService(
                jobRepository,
                runRepository,
                templateRepository,
                testSetItemRepository,
                asyncRunner
        );
        when(runRepository.findByStatus(any(RunStatus.class)))
                .thenReturn(Collections.<BenchmarkRun>emptyList());
        when(runRepository.save(any(BenchmarkRun.class))).thenAnswer(invocation -> {
            BenchmarkRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                ReflectionTestUtils.setField(run, "id", 42L);
            }
            return run;
        });
    }

    // Covers BenchmarkExecutionService#startRun when SQL Lib entries exist.
    @Test
    void shouldStartRunWhenTemplatesExist() {
        BenchmarkJob job = baseJob();
        when(jobRepository.findById(7L)).thenReturn(Optional.of(job));
        when(templateRepository.findAllByOrderByIdAsc()).thenReturn(Arrays.asList(new SqlTemplate()));

        BenchmarkRun run = service.startRun(7L);

        assertEquals(Long.valueOf(42L), run.getId());
        assertEquals(RunStatus.RUNNING, run.getStatus());
        verify(asyncRunner).executeRun(42L);
    }

    // Covers BenchmarkExecutionService#startRun when a linked test set contains items.
    @Test
    void shouldStartRunWhenTestSetContainsItems() {
        BenchmarkJob job = baseJob();
        job.setTestSetId(9L);
        when(jobRepository.findById(7L)).thenReturn(Optional.of(job));
        when(testSetItemRepository.countByTestSetId(9L)).thenReturn(2L);

        BenchmarkRun run = service.startRun(7L);

        assertEquals(RunStatus.RUNNING, run.getStatus());
        verify(asyncRunner).executeRun(42L);
    }

    // Covers BenchmarkExecutionService#startRun empty test-set rejection.
    @Test
    void shouldRejectStartWhenLinkedTestSetIsEmpty() {
        BenchmarkJob job = baseJob();
        job.setTestSetId(9L);
        when(jobRepository.findById(7L)).thenReturn(Optional.of(job));
        when(testSetItemRepository.countByTestSetId(9L)).thenReturn(0L);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.startRun(7L));

        assertEquals("所选测试集为空或不存在，请先从 SQL Lib 选择 SQL", error.getMessage());
        verify(runRepository, never()).save(any(BenchmarkRun.class));
    }

    // Covers BenchmarkExecutionService#startRun empty SQL-Lib rejection.
    @Test
    void shouldRejectStartWhenNoTemplatesExist() {
        when(jobRepository.findById(7L)).thenReturn(Optional.of(baseJob()));
        when(templateRepository.findAllByOrderByIdAsc()).thenReturn(Collections.<SqlTemplate>emptyList());

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.startRun(7L));

        assertEquals("未绑定测试集且 SQL Lib 为空，请先维护 SQL Lib 或关联测试集", error.getMessage());
        verify(runRepository, never()).save(any(BenchmarkRun.class));
    }

    // Covers BenchmarkExecutionService#reconcileStaleRuns stale-run failure recovery.
    @Test
    void shouldMarkStaleRunsFailedAndPopulateMissingErrorSample() {
        BenchmarkRun staleRun = new BenchmarkRun();
        ReflectionTestUtils.setField(staleRun, "id", 3L);
        staleRun.setStatus(RunStatus.RUNNING);
        ReflectionTestUtils.setField(staleRun, "startedAt", Instant.now().minus(1, ChronoUnit.HOURS));
        when(runRepository.findByStatus(RunStatus.RUNNING))
                .thenReturn(Collections.singletonList(staleRun));

        int recovered = service.reconcileStaleRuns();

        assertEquals(1, recovered);
        ArgumentCaptor<BenchmarkRun> runCaptor = ArgumentCaptor.forClass(BenchmarkRun.class);
        verify(runRepository).save(runCaptor.capture());
        BenchmarkRun savedRun = runCaptor.getValue();
        assertEquals(RunStatus.FAILED, savedRun.getStatus());
        assertNotNull(savedRun.getEndedAt());
        assertEquals("Recovered stale RUNNING run after benchmark service restart.", savedRun.getErrorSample());
    }

    // Covers BenchmarkExecutionService#reconcileStaleRuns recovery of recent runs left behind by service restart.
    @Test
    void shouldRecoverRecentRunningRunsStartedBeforeCurrentServiceInstance() {
        BenchmarkRun restartedRun = new BenchmarkRun();
        ReflectionTestUtils.setField(restartedRun, "id", 5L);
        restartedRun.setStatus(RunStatus.RUNNING);
        ReflectionTestUtils.setField(restartedRun, "startedAt", Instant.now().minus(1, ChronoUnit.MINUTES));
        when(runRepository.findByStatus(RunStatus.RUNNING))
                .thenReturn(Collections.singletonList(restartedRun));

        int recovered = service.reconcileStaleRuns();

        assertEquals(1, recovered);
        ArgumentCaptor<BenchmarkRun> runCaptor = ArgumentCaptor.forClass(BenchmarkRun.class);
        verify(runRepository).save(runCaptor.capture());
        assertEquals(RunStatus.FAILED, runCaptor.getValue().getStatus());
    }

    // Covers BenchmarkExecutionService#reconcileStaleRuns existing error-sample preservation.
    @Test
    void shouldPreserveExistingErrorSampleDuringStaleRunRecovery() {
        BenchmarkRun staleRun = new BenchmarkRun();
        ReflectionTestUtils.setField(staleRun, "id", 4L);
        staleRun.setStatus(RunStatus.RUNNING);
        ReflectionTestUtils.setField(staleRun, "startedAt", Instant.now().minus(1, ChronoUnit.HOURS));
        staleRun.setErrorSample("Existing driver timeout");
        when(runRepository.findByStatus(RunStatus.RUNNING))
                .thenReturn(Collections.singletonList(staleRun));

        service.reconcileStaleRuns();

        ArgumentCaptor<BenchmarkRun> runCaptor = ArgumentCaptor.forClass(BenchmarkRun.class);
        verify(runRepository).save(runCaptor.capture());
        assertEquals("Existing driver timeout", runCaptor.getValue().getErrorSample());
    }

    // Covers BenchmarkExecutionService#startRun deferring async launch until transaction commit.
    @Test
    void shouldDeferAsyncLaunchUntilAfterCommitWhenSynchronizationIsActive() {
        BenchmarkJob job = baseJob();
        when(jobRepository.findById(7L)).thenReturn(Optional.of(job));
        when(templateRepository.findAllByOrderByIdAsc()).thenReturn(Arrays.asList(new SqlTemplate()));

        TransactionSynchronizationManager.initSynchronization();
        try {
            BenchmarkRun run = service.startRun(7L);

            assertEquals(Long.valueOf(42L), run.getId());
            verify(asyncRunner, never()).executeRun(anyLong());

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(asyncRunner).executeRun(42L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static BenchmarkJob baseJob() {
        BenchmarkJob job = new BenchmarkJob();
        job.setId(7L);
        job.setName("benchmark-job");
        job.setDataSourceId(12L);
        job.setConcurrentThreads(2);
        job.setRounds(4);
        return job;
    }
}
