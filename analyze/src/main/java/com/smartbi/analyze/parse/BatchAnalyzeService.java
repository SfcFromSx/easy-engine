package com.smartbi.analyze.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BatchAnalyzeService {

    private final SqlStructureAnalyzer sqlStructureAnalyzer;

    public BatchAnalyzeService() {
        this(new SqlStructureAnalyzer());
    }

    public BatchAnalyzeService(SqlStructureAnalyzer sqlStructureAnalyzer) {
        this.sqlStructureAnalyzer = sqlStructureAnalyzer;
    }

    public ParseOutcome analyze(String sql) {
        return sqlStructureAnalyzer.analyze(sql);
    }

    public List<ParseOutcome> analyzeAll(List<String> sqlStatements) {
        if (sqlStatements == null || sqlStatements.isEmpty()) {
            return Collections.emptyList();
        }
        List<ParseOutcome> outcomes = new ArrayList<ParseOutcome>(sqlStatements.size());
        for (String sql : sqlStatements) {
            outcomes.add(analyze(sql));
        }
        return outcomes;
    }
}
