package com.smartbi.analyze.parse;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BatchAnalyzeServiceTest {

    private final BatchAnalyzeService service = new BatchAnalyzeService();

    @Test
    void analyzesSupportedSelectShape() {
        ParseOutcome outcome = service.analyze("SELECT city, count(*) FROM sales GROUP BY city");

        assertEquals(ParseOutcomeStatus.OK, outcome.getStatus());
        assertFalse(outcome.getSignature().getTables().isEmpty());
        assertFalse(outcome.getSignature().getSelectItems().isEmpty());
    }

    @Test
    void batchAnalyzeMatchesSingleAnalyzeShape() {
        List<ParseOutcome> outcomes = service.analyzeAll(Arrays.asList("SELECT 1", "SELECT city FROM sales"));

        assertEquals(2, outcomes.size());
        assertEquals(ParseOutcomeStatus.OK, outcomes.get(0).getStatus());
        assertEquals(ParseOutcomeStatus.OK, outcomes.get(1).getStatus());
    }
}
