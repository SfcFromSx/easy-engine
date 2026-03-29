package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.SqlTemplate;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final SqlTemplateRepository repository;

    public TemplateController(SqlTemplateRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<SqlTemplate> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").ascending());
        if (keyword != null && !keyword.isEmpty()) {
            return repository.searchByKeyword(keyword, pageRequest);
        }
        return repository.findAll(pageRequest);
    }

    @PostMapping
    public SqlTemplate create(@RequestBody SqlTemplate t) {
        t.setId(null);
        return repository.save(t);
    }

    @PutMapping("/{id}")
    public SqlTemplate update(@PathVariable long id, @RequestBody SqlTemplate t) {
        SqlTemplate e = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        e.setName(t.getName());
        e.setSqlText(t.getSqlText());
        e.setWeight(t.getWeight());
        e.setDescription(t.getDescription());
        e.setExecutionMode(t.getExecutionMode());
        e.setParamJson(t.getParamJson());
        return repository.save(e);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        repository.deleteById(id);
    }
}
