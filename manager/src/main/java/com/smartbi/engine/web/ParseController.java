package com.smartbi.engine.web;

import com.smartbi.engine.parse.ParseOutcome;
import com.smartbi.engine.parse.SqlParseService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/parse")
public class ParseController {

    private final SqlParseService sqlParseService;

    public ParseController(SqlParseService sqlParseService) {
        this.sqlParseService = sqlParseService;
    }

    @PostMapping("/preview")
    public Map<String, Object> preview(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        ParseOutcome out = sqlParseService.analyze(sql);
        return Collections.singletonMap("outcome", out);
    }
}
