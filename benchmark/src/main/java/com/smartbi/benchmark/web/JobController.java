package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkJob;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.repo.BenchmarkRunRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final BenchmarkJobRepository repository;
    private final BenchmarkRunRepository runRepository;

    public JobController(BenchmarkJobRepository repository, BenchmarkRunRepository runRepository) {
        this.repository = repository;
        this.runRepository = runRepository;
    }

    @GetMapping
    public List<BenchmarkJob> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public BenchmarkJob get(@PathVariable long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("job not found: " + id));
    }

    @PostMapping
    public BenchmarkJob create(@RequestBody BenchmarkJob job) {
        job.setId(null);
        return repository.save(job);
    }

    @PutMapping("/{id}")
    public BenchmarkJob update(@PathVariable long id, @RequestBody BenchmarkJob job) {
        BenchmarkJob e = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        e.setName(job.getName());
        e.setDataSourceId(job.getDataSourceId());
        e.setConcurrentThreads(job.getConcurrentThreads());
        e.setRounds(job.getRounds());
        e.setStrategy(job.getStrategy());
        e.setTestSetId(job.getTestSetId());
        return repository.save(e);
    }

    @DeleteMapping("/{id}")
    public void remove(@PathVariable long id) {
        if (runRepository.existsByJobId(id)) {
            throw new IllegalStateException("任务已有关联运行记录，暂不支持直接删除。请先保留历史记录或手动清理相关 runs。");
        }
        repository.deleteById(id);
    }
}
