package com.smartbi.analyze.route;

import com.smartbi.analyze.sql.ParsedSql;
import com.smartbi.analyze.sql.SqlCommentParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlRoutingAnalyzerTest {

    private final SqlRoutingAnalyzer analyzer = new SqlRoutingAnalyzer();

    @Test
    void usesEngineAsTheOnlyRoutingSignal() {
        RoutingContext context = new RoutingContext(
                "default",
                Arrays.asList(
                        new DatasourceDescriptor("default", "h2", true),
                        new DatasourceDescriptor("presto_local", "presto", false)
                ),
                Collections.<AccelerationRule>emptyList()
        );
        ParsedSql parsed = SqlCommentParser.parse(
                "/* ENGINE=presto_local YH_RPTSEARCHMODE=dashboard */\n/* YH_TARGET_ENGINE=default */\nSELECT * FROM sales");

        RoutingDecision decision = analyzer.analyze("SELECT * FROM sales", parsed, context);

        assertEquals("presto_local", decision.getDatasourceName());
        assertEquals("/* ENGINE=presto_local */\n/* YH_RPTSEARCHMODE=dashboard */\nSELECT * FROM sales",
                decision.getExecutionSql());
    }

    @Test
    void injectsDefaultEngineWhenQueryHasNoRouteHint() {
        RoutingContext context = new RoutingContext(
                "default",
                Collections.singletonList(new DatasourceDescriptor("default", "h2", true)),
                Collections.<AccelerationRule>emptyList()
        );

        RoutingDecision decision = analyzer.analyze("SELECT * FROM sales", SqlCommentParser.parse("SELECT * FROM sales"), context);

        assertEquals("default", decision.getDatasourceName());
        assertEquals("/* ENGINE=default */\nSELECT * FROM sales", decision.getExecutionSql());
    }

    @Test
    void addsCacheTableCommentWhenAccelerationMatches() {
        RoutingContext context = new RoutingContext(
                "default",
                Collections.singletonList(new DatasourceDescriptor("default", "h2", true)),
                Collections.singletonList(new AccelerationRule("mv_sales", "analytics", "mv_sales", "INSERT INTO analytics.mv_sales SELECT * FROM sales"))
        );

        RoutingDecision decision = analyzer.analyze("SELECT * FROM sales", SqlCommentParser.parse("SELECT * FROM sales"), context);

        assertEquals("analytics.mv_sales", decision.getCacheTable());
        assertEquals("mv_sales", decision.getAccelerationRuleName());
        assertTrue(decision.getExecutionSql().contains("cache-table=analytics.mv_sales"));
    }
}
