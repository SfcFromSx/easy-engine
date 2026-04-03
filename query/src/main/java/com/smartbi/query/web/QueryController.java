package com.smartbi.query.web;

import com.smartbi.query.api.dto.PreparedQueryRequestDto;
import com.smartbi.query.api.dto.SqlResponseStubDto;
import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.service.QueryExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/kylin/api")
public class QueryController {

    private final QueryExecutionService queryExecutionService;
    private final QueryProperties queryProperties;

    public QueryController(QueryExecutionService queryExecutionService,
                           QueryProperties queryProperties) {
        this.queryExecutionService = queryExecutionService;
        this.queryProperties = queryProperties;
    }

    @PostMapping("/query")
    public SqlResponseStubDto query(@RequestBody(required = false) PreparedQueryRequestDto request,
                                    HttpServletRequest servletRequest) {
        rejectTrinoPortTraffic(servletRequest);
        return queryExecutionService.execute(request);
    }

    /**
     * Dummy authentication endpoint to satisfy the Kylin JDBC driver's connection handshake.
     */
    @RequestMapping(value = "/user/authentication", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public String authenticate(HttpServletRequest servletRequest) {
        rejectTrinoPortTraffic(servletRequest);
        return "{\"authenticated\": true, \"userDetails\": {\"username\": \"ADMIN\"}}";
    }

    private void rejectTrinoPortTraffic(HttpServletRequest request) {
        if (request != null && request.getLocalPort() == queryProperties.getTrino().getPort()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
