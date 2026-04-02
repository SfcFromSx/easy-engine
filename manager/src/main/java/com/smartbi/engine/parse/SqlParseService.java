package com.smartbi.engine.parse;

import com.smartbi.analyze.parse.BatchAnalyzeService;
import com.smartbi.analyze.parse.ParseOutcome;
import org.springframework.stereotype.Service;

@Service
public class SqlParseService {

    private final BatchAnalyzeService batchAnalyzeService;

    public SqlParseService() {
        this(new BatchAnalyzeService());
    }

    SqlParseService(BatchAnalyzeService batchAnalyzeService) {
        this.batchAnalyzeService = batchAnalyzeService;
    }

    public ParseOutcome analyze(String sql) {
        return batchAnalyzeService.analyze(sql);
    }
}
