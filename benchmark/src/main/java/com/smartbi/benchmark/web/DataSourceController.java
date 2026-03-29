package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkDataSource;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.web.dto.QueryRequest;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

@RestController
@RequestMapping("/api/v1/datasources")
public class DataSourceController {

    private final BenchmarkDataSourceRepository repository;
    private final BenchmarkJobRepository jobRepository;
    private final BenchmarkQueryService queryService;

    public DataSourceController(BenchmarkDataSourceRepository repository,
                                BenchmarkJobRepository jobRepository,
                                BenchmarkQueryService queryService) {
        this.repository = repository;
        this.jobRepository = jobRepository;
        this.queryService = queryService;
    }

    @GetMapping
    public List<BenchmarkDataSource> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public BenchmarkDataSource get(@PathVariable long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("datasource not found"));
    }

    @PostMapping
    public BenchmarkDataSource create(@RequestBody BenchmarkDataSource ds) {
        ds.setId(null);
        return repository.save(ds);
    }

    @PutMapping("/{id}")
    public BenchmarkDataSource update(@PathVariable long id, @RequestBody BenchmarkDataSource ds) {
        BenchmarkDataSource e = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        e.setName(ds.getName());
        e.setJdbcUrl(ds.getJdbcUrl());
        e.setJdbcUser(ds.getJdbcUser());
        e.setJdbcPassword(ds.getJdbcPassword());
        e.setDriverClass(ds.getDriverClass());
        return repository.save(e);
    }

    @DeleteMapping("/{id}")
    public void remove(@PathVariable long id) {
        if (jobRepository.existsByDataSourceId(id)) {
            throw new IllegalStateException("该数据源仍被压测任务引用，无法删除。请先修改或删除相关任务。");
        }
        repository.deleteById(id);
    }

    @PostMapping("/{id}/query")
    public BenchmarkQueryService.QueryResponse executeQuery(@PathVariable long id, @RequestBody QueryRequest req) {
        return queryService.executeQuery(id, req.getSql());
    }

    @PostMapping("/test")
    public String testConnection(@RequestBody BenchmarkDataSource ds) {
        try {
            Class.forName(ds.getDriverClass());
            try (Connection conn = DriverManager.getConnection(ds.getJdbcUrl(), 
                    ds.getJdbcUser() != null ? ds.getJdbcUser() : "", 
                    ds.getJdbcPassword() != null ? ds.getJdbcPassword() : "")) {
                return "SUCCESS";
            }
        } catch (Exception e) {
            return "FAILED: " + e.getMessage();
        }
    }
}
