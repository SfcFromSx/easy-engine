package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkRun;
import com.smartbi.benchmark.domain.RunStatus;
import com.smartbi.benchmark.repo.BenchmarkRunRepository;
import com.smartbi.benchmark.report.BenchmarkRunReportService;
import com.smartbi.benchmark.run.BenchmarkExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/runs")
public class RunController {

    private final BenchmarkExecutionService executionService;
    private final BenchmarkRunRepository runRepository;
    private final BenchmarkRunReportService reportService;

    public RunController(BenchmarkExecutionService executionService,
                         BenchmarkRunRepository runRepository,
                         BenchmarkRunReportService reportService) {
        this.executionService = executionService;
        this.runRepository = runRepository;
        this.reportService = reportService;
    }

    @PostMapping("/start")
    public BenchmarkRun start(@RequestBody Map<String, Long> body) {
        long jobId = body.get("jobId");
        return executionService.startRun(jobId);
    }

    @GetMapping("/{id}")
    public BenchmarkRun get(@PathVariable long id) {
        return runRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    /**
     * 当前 Run + 同任务上一次更早的 Run + 结构化 delta，供管理端对比展示。
     */
    @GetMapping("/{id}/context")
    public Map<String, Object> context(@PathVariable long id) {
        BenchmarkRun run = runRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("run", run);
        body.put("failureBreakdown", reportService.extractFailureBreakdown(run.getEvaluationJson()));
        BenchmarkRun previous = runRepository
                .findFirstByJobIdAndStartedAtLessThanOrderByStartedAtDesc(run.getJobId(), run.getStartedAt())
                .orElse(null);
        
        // F3: Auto-Baseline Fallback - If the immediate previous is not COMPLETED, find the last successful one
        if (previous == null || previous.getStatus() != com.smartbi.benchmark.domain.RunStatus.COMPLETED) {
            previous = runRepository.findFirstByJobIdAndStatusAndIdNotOrderByStartedAtDesc(
                run.getJobId(), com.smartbi.benchmark.domain.RunStatus.COMPLETED, run.getId()
            ).orElse(previous);
        }
        body.put("previousRun", previous);
        body.put("comparisonDelta", reportService.buildComparisonDelta(run, previous));
        return body;
    }

    @GetMapping
    public Page<BenchmarkRun> list(@RequestParam(required = false) Long jobId,
                                   @RequestParam(required = false) RunStatus status,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        if (jobId != null && status != null) {
            return runRepository.findByJobIdAndStatusOrderByStartedAtDesc(jobId, status, pageRequest);
        }
        if (jobId != null) {
            return runRepository.findByJobIdOrderByStartedAtDesc(jobId, pageRequest);
        }
        if (status != null) {
            return runRepository.findByStatusOrderByStartedAtDesc(status, pageRequest);
        }
        return runRepository.findAll(pageRequest);
    }

    @GetMapping("/active")
    public BenchmarkRun getActive() {
        executionService.reconcileStaleRuns();
        return runRepository.findFirstByStatusOrderByStartedAtDesc(com.smartbi.benchmark.domain.RunStatus.RUNNING)
                .orElse(null);
    }
}
