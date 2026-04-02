package com.smartbi.engine.web;

import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationTable;
import com.smartbi.engine.datasource.QueryDatasourceConfigService;
import com.smartbi.engine.repo.AccelerationTableRepository;
import com.smartbi.engine.web.dto.QueryRoutingAccelerationRuleDto;
import com.smartbi.engine.web.dto.QueryRoutingContextDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/query-routing-context")
public class QueryRoutingContextController {

    private final QueryDatasourceConfigService queryDatasourceConfigService;
    private final AccelerationTableRepository accelerationTableRepository;

    public QueryRoutingContextController(QueryDatasourceConfigService queryDatasourceConfigService,
                                         AccelerationTableRepository accelerationTableRepository) {
        this.queryDatasourceConfigService = queryDatasourceConfigService;
        this.accelerationTableRepository = accelerationTableRepository;
    }

    @GetMapping
    public QueryRoutingContextDto getRoutingContext() {
        QueryRoutingContextDto dto = new QueryRoutingContextDto();
        dto.setDatasources(queryDatasourceConfigService.list());
        dto.setAccelerationRules(toAccelerationRules(accelerationTableRepository.findAllByOrderByUpdatedAtDesc()));
        return dto;
    }

    private static List<QueryRoutingAccelerationRuleDto> toAccelerationRules(List<AccelerationTable> tables) {
        List<QueryRoutingAccelerationRuleDto> rules = new ArrayList<QueryRoutingAccelerationRuleDto>();
        for (AccelerationTable table : tables) {
            if (table == null || table.getStatus() != AccelerationStatus.ACTIVE) {
                continue;
            }
            QueryRoutingAccelerationRuleDto rule = new QueryRoutingAccelerationRuleDto();
            rule.setName(table.getName());
            rule.setSchemaName(table.getSchemaName());
            rule.setTableName(table.getName());
            rule.setRefreshSql(table.getRefreshSql());
            rules.add(rule);
        }
        return rules;
    }
}
