package com.smartbi.benchmark.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "benchmark_run")
public class BenchmarkRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RunStatus status = RunStatus.RUNNING;

    @Column(name = "total_queries")
    private Long totalQueries;

    @Column(name = "success_count")
    private Long successCount;

    @Column(name = "error_count")
    private Long errorCount;

    @Column(name = "duration_ms")
    private Long durationMs;

    private Double qps;

    @Column(name = "p50_ms")
    private Double p50Ms;

    @Column(name = "p95_ms")
    private Double p95Ms;

    @Column(name = "p99_ms")
    private Double p99Ms;

    @Column(name = "error_sample", columnDefinition = "TEXT")
    private String errorSample;

    /** 压测开始时任务配置快照（JSON），便于与 evaluation 一并做版本对比 */
    @Column(name = "job_snapshot_json", columnDefinition = "TEXT")
    private String jobSnapshotJson;

    /** 结构化评价与 JDBC 对比提示（JSON） */
    @Column(name = "evaluation_json", columnDefinition = "TEXT")
    private String evaluationJson;

    @Column(name = "current_progress")
    private Integer currentProgress = 0;

    @Column(name = "total_target")
    private Integer totalTarget = 0;

    public Long getId() {
        return id;
    }

    public Integer getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(Integer currentProgress) {
        this.currentProgress = currentProgress;
    }

    public Integer getTotalTarget() {
        return totalTarget;
    }

    public void setTotalTarget(Integer totalTarget) {
        this.totalTarget = totalTarget;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public Long getTotalQueries() {
        return totalQueries;
    }

    public void setTotalQueries(Long totalQueries) {
        this.totalQueries = totalQueries;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }

    public Long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Long errorCount) {
        this.errorCount = errorCount;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Double getQps() {
        return qps;
    }

    public void setQps(Double qps) {
        this.qps = qps;
    }

    public Double getP50Ms() {
        return p50Ms;
    }

    public void setP50Ms(Double p50Ms) {
        this.p50Ms = p50Ms;
    }

    public Double getP95Ms() {
        return p95Ms;
    }

    public void setP95Ms(Double p95Ms) {
        this.p95Ms = p95Ms;
    }

    public Double getP99Ms() {
        return p99Ms;
    }

    public void setP99Ms(Double p99Ms) {
        this.p99Ms = p99Ms;
    }

    public String getErrorSample() {
        return errorSample;
    }

    public void setErrorSample(String errorSample) {
        this.errorSample = errorSample;
    }

    public String getJobSnapshotJson() {
        return jobSnapshotJson;
    }

    public void setJobSnapshotJson(String jobSnapshotJson) {
        this.jobSnapshotJson = jobSnapshotJson;
    }

    public String getEvaluationJson() {
        return evaluationJson;
    }

    public void setEvaluationJson(String evaluationJson) {
        this.evaluationJson = evaluationJson;
    }
}
