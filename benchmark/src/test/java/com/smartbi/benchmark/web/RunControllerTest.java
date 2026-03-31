package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkRun;
import com.smartbi.benchmark.domain.RunStatus;
import com.smartbi.benchmark.repo.BenchmarkRunRepository;
import com.smartbi.benchmark.report.BenchmarkRunReportService;
import com.smartbi.benchmark.run.BenchmarkExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunControllerTest {

    // Covers RunController#start request delegation to BenchmarkExecutionService.
    @Test
    void shouldDelegateRunStartRequests() {
        BenchmarkExecutionService executionService = mock(BenchmarkExecutionService.class);
        BenchmarkRunRepository runRepository = mock(BenchmarkRunRepository.class);
        BenchmarkRunReportService reportService = mock(BenchmarkRunReportService.class);
        RunController controller = new RunController(executionService, runRepository, reportService);
        BenchmarkRun startedRun = new BenchmarkRun();
        ReflectionTestUtils.setField(startedRun, "id", 11L);
        when(executionService.startRun(7L)).thenReturn(startedRun);

        BenchmarkRun run = controller.start(Collections.singletonMap("jobId", 7L));

        assertSame(startedRun, run);
        verify(executionService).startRun(7L);
    }

    // Covers RunController#context baseline fallback to the latest completed run.
    @Test
    void shouldFallbackToLastCompletedBaselineWhenImmediatePreviousRunFailed() {
        BenchmarkExecutionService executionService = mock(BenchmarkExecutionService.class);
        BenchmarkRunRepository runRepository = mock(BenchmarkRunRepository.class);
        BenchmarkRunReportService reportService = mock(BenchmarkRunReportService.class);
        RunController controller = new RunController(executionService, runRepository, reportService);

        BenchmarkRun current = run(21L, RunStatus.COMPLETED, Instant.parse("2026-03-31T10:00:00Z"));
        current.setJobId(9L);
        current.setEvaluationJson("{\"diagnostics\":{\"failureBreakdown\":{\"groups\":[]}}}");
        BenchmarkRun previousFailed = run(20L, RunStatus.FAILED, Instant.parse("2026-03-31T09:00:00Z"));
        previousFailed.setJobId(9L);
        BenchmarkRun completedBaseline = run(19L, RunStatus.COMPLETED, Instant.parse("2026-03-31T08:00:00Z"));
        completedBaseline.setJobId(9L);
        Map<String, Object> failureBreakdown = new LinkedHashMap<String, Object>();
        failureBreakdown.put("groupCount", 0);
        Map<String, Object> comparison = new LinkedHashMap<String, Object>();
        comparison.put("available", true);
        comparison.put("baselineRunId", 19L);

        when(runRepository.findById(21L)).thenReturn(Optional.of(current));
        when(runRepository.findFirstByJobIdAndStartedAtLessThanOrderByStartedAtDesc(9L, current.getStartedAt()))
                .thenReturn(Optional.of(previousFailed));
        when(runRepository.findFirstByJobIdAndStatusAndIdNotOrderByStartedAtDesc(9L, RunStatus.COMPLETED, 21L))
                .thenReturn(Optional.of(completedBaseline));
        when(reportService.extractFailureBreakdown(current.getEvaluationJson())).thenReturn(failureBreakdown);
        when(reportService.buildComparisonDelta(current, completedBaseline)).thenReturn(comparison);

        Map<String, Object> body = controller.context(21L);

        assertSame(current, body.get("run"));
        assertSame(completedBaseline, body.get("previousRun"));
        assertSame(failureBreakdown, body.get("failureBreakdown"));
        assertSame(comparison, body.get("comparisonDelta"));
    }

    // Covers RunController#list global, per-job, status-only, combined filters, and RunController#getActive stale-run recovery.
    @Test
    void shouldReturnPagedRunsAndExposeLatestRecoveredActiveRun() {
        BenchmarkExecutionService executionService = mock(BenchmarkExecutionService.class);
        BenchmarkRunRepository runRepository = mock(BenchmarkRunRepository.class);
        BenchmarkRunReportService reportService = mock(BenchmarkRunReportService.class);
        RunController controller = new RunController(executionService, runRepository, reportService);
        BenchmarkRun running = run(30L, RunStatus.RUNNING, Instant.parse("2026-03-31T10:15:00Z"));
        when(runRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<BenchmarkRun>(Arrays.asList(running)));
        when(runRepository.findByJobIdOrderByStartedAtDesc(eq(6L), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<BenchmarkRun>(Arrays.asList(running)));
        when(runRepository.findByStatusOrderByStartedAtDesc(eq(RunStatus.FAILED), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<BenchmarkRun>(Arrays.asList(running)));
        when(runRepository.findByJobIdAndStatusOrderByStartedAtDesc(eq(6L), eq(RunStatus.COMPLETED), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<BenchmarkRun>(Arrays.asList(running)));
        doReturn(2).when(executionService).reconcileStaleRuns();
        when(runRepository.findFirstByStatusOrderByStartedAtDesc(RunStatus.RUNNING))
                .thenReturn(Optional.of(running));

        assertEquals(1, controller.list(null, null, 0, 20).getTotalElements());
        assertEquals(1, controller.list(6L, null, 0, 20).getTotalElements());
        assertEquals(1, controller.list(null, RunStatus.FAILED, 0, 20).getTotalElements());
        assertEquals(1, controller.list(6L, RunStatus.COMPLETED, 0, 20).getTotalElements());
        assertSame(running, controller.getActive());
        verify(runRepository).findAll(any(org.springframework.data.domain.Pageable.class));
        verify(runRepository).findByJobIdOrderByStartedAtDesc(eq(6L), any(org.springframework.data.domain.Pageable.class));
        verify(runRepository).findByStatusOrderByStartedAtDesc(eq(RunStatus.FAILED), any(org.springframework.data.domain.Pageable.class));
        verify(runRepository).findByJobIdAndStatusOrderByStartedAtDesc(eq(6L), eq(RunStatus.COMPLETED), any(org.springframework.data.domain.Pageable.class));
        verify(executionService).reconcileStaleRuns();
        verify(runRepository).findFirstByStatusOrderByStartedAtDesc(RunStatus.RUNNING);

        when(runRepository.findFirstByStatusOrderByStartedAtDesc(RunStatus.RUNNING)).thenReturn(Optional.empty());
        assertNull(controller.getActive());
    }

    private static BenchmarkRun run(Long id, RunStatus status, Instant startedAt) {
        BenchmarkRun run = new BenchmarkRun();
        ReflectionTestUtils.setField(run, "id", id);
        run.setStatus(status);
        ReflectionTestUtils.setField(run, "startedAt", startedAt);
        return run;
    }
}
