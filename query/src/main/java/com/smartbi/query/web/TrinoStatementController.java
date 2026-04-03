package com.smartbi.query.web;

import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.trino.TrinoStatementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@RestController
public class TrinoStatementController {

    private static final String TRINO_USER_HEADER = "X-Trino-User";

    private final TrinoStatementService trinoStatementService;
    private final QueryProperties queryProperties;

    public TrinoStatementController(TrinoStatementService trinoStatementService,
                                    QueryProperties queryProperties) {
        this.trinoStatementService = trinoStatementService;
        this.queryProperties = queryProperties;
    }

    @PostMapping(path = "/v1/statement", consumes = "text/plain", produces = "application/json")
    public ResponseEntity<String> statement(@RequestBody(required = false) String sql,
                                            HttpServletRequest request) {
        requireTrinoPort(request);
        enforceUserHeader(request);
        return trinoStatementService.execute(sql, request);
    }

    private void requireTrinoPort(HttpServletRequest request) {
        if (request == null || request.getLocalPort() != queryProperties.getTrino().getPort()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private void enforceUserHeader(HttpServletRequest request) {
        String expectedUser = queryProperties.getAuth().getUsername();
        if (!StringUtils.hasText(expectedUser)) {
            return;
        }
        String actualUser = request == null ? null : request.getHeader(TRINO_USER_HEADER);
        if (!expectedUser.equals(actualUser)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
