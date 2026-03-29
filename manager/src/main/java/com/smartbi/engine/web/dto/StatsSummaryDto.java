package com.smartbi.engine.web.dto;

import java.time.Instant;

public class StatsSummaryDto {

    private long totalTraces;
    private long parseOk;
    private long parseError;
    private long patternCount;
    private long activeAccelerationCount;
    private long draftAccelerationCount;
    private long cacheHitCount;
    private Instant lastTraceAt;

    public long getTotalTraces() {
        return totalTraces;
    }

    public void setTotalTraces(long totalTraces) {
        this.totalTraces = totalTraces;
    }

    public long getParseOk() {
        return parseOk;
    }

    public void setParseOk(long parseOk) {
        this.parseOk = parseOk;
    }

    public long getParseError() {
        return parseError;
    }

    public void setParseError(long parseError) {
        this.parseError = parseError;
    }

    public long getPatternCount() {
        return patternCount;
    }

    public void setPatternCount(long patternCount) {
        this.patternCount = patternCount;
    }

    public long getActiveAccelerationCount() {
        return activeAccelerationCount;
    }

    public void setActiveAccelerationCount(long activeAccelerationCount) {
        this.activeAccelerationCount = activeAccelerationCount;
    }

    public long getDraftAccelerationCount() {
        return draftAccelerationCount;
    }

    public void setDraftAccelerationCount(long draftAccelerationCount) {
        this.draftAccelerationCount = draftAccelerationCount;
    }

    public long getCacheHitCount() {
        return cacheHitCount;
    }

    public void setCacheHitCount(long cacheHitCount) {
        this.cacheHitCount = cacheHitCount;
    }

    public Instant getLastTraceAt() {
        return lastTraceAt;
    }

    public void setLastTraceAt(Instant lastTraceAt) {
        this.lastTraceAt = lastTraceAt;
    }
}
