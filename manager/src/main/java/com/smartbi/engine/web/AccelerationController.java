package com.smartbi.engine.web;

import com.smartbi.engine.accel.AccelerationService;
import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationTable;
import com.smartbi.engine.web.dto.AccelerationTableUpsertRequest;
import com.smartbi.engine.web.dto.CreateAccelerationFromPatternRequest;
import com.smartbi.engine.web.dto.UpdateAccelerationStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/acceleration-tables")
public class AccelerationController {

    private final AccelerationService accelerationService;

    public AccelerationController(AccelerationService accelerationService) {
        this.accelerationService = accelerationService;
    }

    @GetMapping
    public Page<AccelerationTable> list(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return accelerationService.list(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    @PostMapping
    public AccelerationTable create(@RequestBody AccelerationTableUpsertRequest request) {
        return accelerationService.createManual(
                request.getName(),
                request.getSchemaName(),
                request.getDdlText(),
                request.getRefreshSql(),
                request.getCronExpr());
    }

    @PutMapping("/{id}")
    public AccelerationTable update(@PathVariable long id, @RequestBody AccelerationTableUpsertRequest request) {
        return accelerationService.updateManual(
                id,
                request.getName(),
                request.getSchemaName(),
                request.getDdlText(),
                request.getRefreshSql(),
                request.getCronExpr());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        accelerationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/from-pattern")
    public AccelerationTable fromPattern(@RequestBody CreateAccelerationFromPatternRequest request) {
        if (request.getPatternStatsId() == null) {
            throw new IllegalArgumentException("patternStatsId is required");
        }
        if (request.getTableName() == null) {
            throw new IllegalArgumentException("tableName is required");
        }
        return accelerationService.createFromPattern(
                request.getPatternStatsId(),
                request.getTableName(),
                request.getSchemaName());
    }

    @PatchMapping("/{id}/status")
    public AccelerationTable status(@PathVariable long id, @RequestBody UpdateAccelerationStatusRequest request) {
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("status is required");
        }
        return accelerationService.updateStatus(id, AccelerationStatus.valueOf(request.getStatus()));
    }
}
