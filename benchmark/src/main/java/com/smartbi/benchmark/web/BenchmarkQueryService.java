package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkDataSource;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BenchmarkQueryService {

    private final BenchmarkDataSourceRepository dataSourceRepository;

    public BenchmarkQueryService(BenchmarkDataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    public QueryResponse executeQuery(long dataSourceId, String sql) {
        BenchmarkDataSource ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + dataSourceId));
        
        long start = System.currentTimeMillis();
        try {
            Class.forName(ds.getDriverClass());
            try (Connection conn = DriverManager.getConnection(ds.getJdbcUrl(), 
                    ds.getJdbcUser() != null ? ds.getJdbcUser() : "", 
                    ds.getJdbcPassword() != null ? ds.getJdbcPassword() : "");
                 Statement stmt = conn.createStatement()) {
                
                stmt.setMaxRows(50); // Limit results for safety
                boolean isResultSet = stmt.execute(sql);
                long latency = System.currentTimeMillis() - start;

                if (isResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        ResultSetMetaData md = rs.getMetaData();
                        int cols = md.getColumnCount();
                        List<String> headers = new ArrayList<>();
                        for (int i = 1; i <= cols; i++) {
                            headers.add(md.getColumnLabel(i));
                        }

                        List<Map<String, Object>> rows = new ArrayList<>();
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= cols; i++) {
                                row.put(headers.get(i - 1), rs.getObject(i));
                            }
                            rows.add(row);
                        }
                        return new QueryResponse(headers, rows, latency, null);
                    }
                } else {
                    return new QueryResponse(null, null, latency, "Affected rows: " + stmt.getUpdateCount());
                }
            }
        } catch (Exception e) {
            return new QueryResponse(null, null, System.currentTimeMillis() - start, "Error: " + e.getMessage());
        }
    }

    public static class QueryResponse {
        public List<String> headers;
        public List<Map<String, Object>> rows;
        public long latencyMs;
        public String message;

        public QueryResponse(List<String> headers, List<Map<String, Object>> rows, long latencyMs, String message) {
            this.headers = headers;
            this.rows = rows;
            this.latencyMs = latencyMs;
            this.message = message;
        }
    }
}
