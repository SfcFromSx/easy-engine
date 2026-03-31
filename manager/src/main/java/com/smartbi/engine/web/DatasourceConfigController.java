package com.smartbi.engine.web;

import com.smartbi.engine.datasource.QueryDatasourceConfig;
import com.smartbi.engine.datasource.QueryDatasourceConfigService;
import com.smartbi.engine.web.dto.QueryDatasourceConfigUpsertRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/query-datasources")
public class DatasourceConfigController {

    private final QueryDatasourceConfigService service;

    public DatasourceConfigController(QueryDatasourceConfigService service) {
        this.service = service;
    }

    @GetMapping
    public List<QueryDatasourceConfig> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QueryDatasourceConfig> get(@PathVariable long id) {
        return service.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public QueryDatasourceConfig create(@RequestBody QueryDatasourceConfigUpsertRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public QueryDatasourceConfig update(@PathVariable long id, @RequestBody QueryDatasourceConfigUpsertRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
