package com.smartbi.benchmark.web.dto;

import java.util.List;
import java.util.Map;

public class QueryResponse {
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private long latencyMs;
    private int rowCount;
    private String error;

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }

    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
