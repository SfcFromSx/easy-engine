package com.smartbi.engine.web;

import com.smartbi.engine.accel.AccelerationService;
import com.smartbi.engine.domain.SqlPatternStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patterns")
public class PatternController {

    private final AccelerationService accelerationService;

    public PatternController(AccelerationService accelerationService) {
        this.accelerationService = accelerationService;
    }

    @GetMapping("/top")
    public Page<SqlPatternStats> top(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String fingerprint,
                                     @RequestParam(required = false) String sqlKeyword,
                                     @RequestParam(required = false) Long minExecutionCount) {
        return accelerationService.topPatterns(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executionCount")),
                fingerprint,
                sqlKeyword,
                minExecutionCount);
    }
}
