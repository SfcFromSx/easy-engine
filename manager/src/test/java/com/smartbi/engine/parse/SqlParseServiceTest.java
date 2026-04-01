package com.smartbi.engine.parse;

import com.smartbi.engine.domain.ParseStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlParseServiceTest {

    private final SqlParseService service = new SqlParseService();

    @Test
    // Covers SqlParseService#analyze select branch.
    void parsesSimpleSelect() {
        ParseOutcome out = service.analyze("SELECT a, count(*) FROM t1 GROUP BY a");
        assertEquals(ParseStatus.OK, out.getStatus());
        assertNotNull(out.getSignature());
        assertFalse(out.getSignature().getTables().isEmpty());
        assertFalse(out.getSignature().getSelectItems().isEmpty());
    }

    @Test
    // Covers SqlParseService#analyze skipped branch.
    void skippedOnEmpty() {
        ParseOutcome out = service.analyze("   ");
        assertEquals(ParseStatus.SKIPPED, out.getStatus());
    }

    @Test
    // Covers SqlParseService#analyze order-by branch.
    void parsesOrderByQueryAndKeepsRootKind() {
        ParseOutcome out = service.analyze("SELECT city FROM sales ORDER BY city");

        assertEquals(ParseStatus.OK, out.getStatus());
        assertEquals("ORDER_BY", out.getSignature().getRootKind());
        assertFalse(out.getSignature().getSelectItems().isEmpty());
    }

    @Test
    // Covers SqlParseService#collectFrom nested subquery traversal with current aggregate gap.
    void parsesNestedSubqueryInFromClauseButMissesAggregateExtraction() {
        ParseOutcome out = service.analyze(
                "SELECT region, COUNT(*) " +
                        "FROM (SELECT region FROM sales) nested_sales " +
                        "GROUP BY region");

        assertEquals(ParseStatus.OK, out.getStatus());
        assertEquals("SELECT", out.getSignature().getRootKind());
        assertEquals(java.util.Arrays.asList("sales"), out.getSignature().getTables());
        assertTrue(out.getSignature().getAggregates().isEmpty());
    }

    @Test
    // Covers SqlParseService#analyze CTE root fallback.
    void cteQueryKeepsRootKindButMissesBaseTables() {
        ParseOutcome out = service.analyze(
                "WITH regional_sales AS (" +
                        "SELECT region, SUM(amount) AS total_amount " +
                        "FROM sales " +
                        "GROUP BY region" +
                        ") " +
                        "SELECT region FROM regional_sales");

        assertEquals(ParseStatus.OK, out.getStatus());
        assertEquals("WITH", out.getSignature().getRootKind());
        assertTrue(out.getSignature().getTables().isEmpty());
        assertTrue(out.getSignature().getAggregates().isEmpty());
    }

    @Test
    // Covers SqlParseService#analyze UNION root fallback.
    void unionQueryKeepsRootKindButDoesNotCollectBranchTables() {
        ParseOutcome out = service.analyze("SELECT city FROM sales UNION SELECT city FROM archive_sales");

        assertEquals(ParseStatus.OK, out.getStatus());
        assertEquals("UNION", out.getSignature().getRootKind());
        assertTrue(out.getSignature().getTables().isEmpty());
    }

    @Test
    // Covers SqlParseService#analyze non-SELECT statement fallback.
    void nonSelectStatementKeepsRootKindWithoutLineage() {
        ParseOutcome out = service.analyze("INSERT INTO summary_table SELECT city FROM sales");

        assertEquals(ParseStatus.OK, out.getStatus());
        assertEquals("INSERT", out.getSignature().getRootKind());
        assertTrue(out.getSignature().getTables().isEmpty());
    }

    @Test
    // Covers SqlParseService#analyze error branch.
    void returnsErrorForInvalidSql() {
        ParseOutcome out = service.analyze("SELECT FROM");

        assertEquals(ParseStatus.ERROR, out.getStatus());
        assertNotNull(out.getErrorMessage());
    }
}
