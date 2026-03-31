package com.smartbi.query.route;

import com.smartbi.query.config.ManagerConfigClient;
import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.datasource.ManagedDataSourceRegistry;
import com.smartbi.query.parsing.SqlCommentParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlRouteServiceTest {

    // Covers SqlRouteService#routeAndRewrite default-routing branch.
    @Test
    void shouldRouteToDefaultDatasourceWhenNoHintsArePresent() {
        SqlRouteService service = new SqlRouteService(registry(managerConfigs("default", "h2", true, "presto_local", "PRESTO", false)));

        RoutedSql routed = service.routeAndRewrite("SELECT * FROM SALES", SqlCommentParser.parse("SELECT * FROM SALES"));

        assertEquals("default", routed.datasourceName);
        assertEquals("h2", routed.datasourceType);
        assertEquals("SELECT * FROM SALES", routed.executionSql);
    }

    // Covers SqlRouteService#routeAndRewrite preserved-metadata precedence over engine hints.
    @Test
    void shouldPreferYhTargetEngineOverDriverEngineHint() {
        SqlRouteService service = new SqlRouteService(registry(managerConfigs("default", "h2", true, "presto_local", "PRESTO", false)));
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse("/* YH_TARGET_ENGINE=presto_local */ -- engine=default\nSELECT * FROM SALES");

        RoutedSql routed = service.routeAndRewrite("SELECT * FROM SALES", parsed);

        assertEquals("presto_local", routed.datasourceName);
        assertEquals("PRESTO", routed.datasourceType);
    }

    // Covers SqlRouteService#routeAndRewrite unknown-datasource fallback branch.
    @Test
    void shouldFallbackToDefaultDatasourceWhenTargetIsUnknown() {
        SqlRouteService service = new SqlRouteService(registry(managerConfigs("default", "h2", true)));
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse("/* YH_TARGET_ENGINE=missing */ SELECT * FROM SALES");

        RoutedSql routed = service.routeAndRewrite("SELECT * FROM SALES", parsed);

        assertEquals("default", routed.datasourceName);
        assertEquals("h2", routed.datasourceType);
    }

    // Covers SqlRouteService#routeAndRewrite adapter lookup for normalized datasource types.
    @Test
    void shouldUseDatasourceTypeLookupCaseInsensitively() {
        SqlRouteService service = new SqlRouteService(registry(managerConfigs("default", "PRESTO", true)));

        RoutedSql routed = service.routeAndRewrite("SELECT * FROM SALES", SqlCommentParser.parse("SELECT * FROM SALES"));

        assertEquals("PRESTO", routed.datasourceType);
        assertEquals("SELECT * FROM SALES", routed.executionSql);
    }

    // Covers SqlRouteService#routeAndRewrite parsed-null fallback branch.
    @Test
    void shouldHandleNullParsedSqlByPassingThroughOriginalSql() {
        SqlRouteService service = new SqlRouteService(registry(managerConfigs("default", "h2", true)));

        RoutedSql routed = service.routeAndRewrite("SELECT * FROM SALES", null);

        assertEquals("default", routed.datasourceName);
        assertEquals("SELECT * FROM SALES", routed.executionSql);
    }

    private static ManagedDataSourceRegistry registry(List<ManagerConfigClient.ManagerDatasourceConfig> configs) {
        QueryProperties properties = new QueryProperties();
        properties.setManagerUrl("http://localhost:8090");
        return new ManagedDataSourceRegistry(properties, new StubManagerConfigClient(configs));
    }

    private static List<ManagerConfigClient.ManagerDatasourceConfig> managerConfigs(Object... values) {
        java.util.ArrayList<ManagerConfigClient.ManagerDatasourceConfig> configs =
                new java.util.ArrayList<ManagerConfigClient.ManagerDatasourceConfig>();
        for (int i = 0; i < values.length; i += 3) {
            ManagerConfigClient.ManagerDatasourceConfig config = new ManagerConfigClient.ManagerDatasourceConfig();
            config.setName((String) values[i]);
            config.setType((String) values[i + 1]);
            config.setDriverClass("org.h2.Driver");
            config.setJdbcUrl("jdbc:h2:mem:" + values[i]);
            config.setUsername("sa");
            config.setPassword("");
            config.setIsDefault((Boolean) values[i + 2]);
            configs.add(config);
        }
        return configs;
    }

    private static class StubManagerConfigClient extends ManagerConfigClient {
        private final List<ManagerDatasourceConfig> configs;

        private StubManagerConfigClient(List<ManagerDatasourceConfig> configs) {
            super(new RestTemplateBuilder(), new QueryProperties());
            this.configs = configs == null ? Collections.<ManagerDatasourceConfig>emptyList() : configs;
        }

        @Override
        public List<ManagerDatasourceConfig> fetchDatasourceConfigs() {
            return configs;
        }
    }
}
