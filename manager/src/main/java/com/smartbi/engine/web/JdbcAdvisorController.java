package com.smartbi.engine.web;

import com.smartbi.engine.jdbc.JdbcSqlAdvisorService;
import com.smartbi.engine.jdbc.dto.SqlRewriteRequest;
import com.smartbi.engine.jdbc.dto.SqlRewriteResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制面 API：供 JDBC 驱动在发送前咨询 SQL / 注释改写。Engine 不执行该 SQL。
 */
@RestController
@RequestMapping("/api/v1/jdbc")
public class JdbcAdvisorController {

    private final JdbcSqlAdvisorService jdbcSqlAdvisorService;

    public JdbcAdvisorController(JdbcSqlAdvisorService jdbcSqlAdvisorService) {
        this.jdbcSqlAdvisorService = jdbcSqlAdvisorService;
    }

    @PostMapping("/sql-rewrite")
    public SqlRewriteResponse sqlRewrite(@RequestBody SqlRewriteRequest request) {
        return jdbcSqlAdvisorService.adviseRewrite(request);
    }
}
