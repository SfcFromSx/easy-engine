package com.smartbi.query.web;

import com.smartbi.query.api.dto.PreparedQueryRequestDto;
import com.smartbi.query.api.dto.SqlResponseStubDto;
import com.smartbi.query.service.QueryExecutionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kylin/api")
public class QueryController {

    private final QueryExecutionService queryExecutionService;

    public QueryController(QueryExecutionService queryExecutionService) {
        this.queryExecutionService = queryExecutionService;
    }

    @PostMapping("/query")
    public SqlResponseStubDto query(@RequestBody(required = false) PreparedQueryRequestDto request) {
        return queryExecutionService.execute(request);
    }

    /**
     * Dummy authentication endpoint to satisfy the Kylin JDBC driver's connection handshake.
     */
    @RequestMapping(value = "/user/authentication", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public String authenticate() {
        return "{\"authenticated\": true, \"userDetails\": {\"username\": \"ADMIN\"}}";
    }
}
