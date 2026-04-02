package com.smartbi.query.route;

import com.smartbi.query.config.ManagerConfigClient;
import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.datasource.ManagedDataSourceRegistry;
import com.smartbi.query.parsing.SqlCommentParser;
import com.smartbi.query.support.QueryTestFixtures;
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
        assertEquals("/* YH_TARGET_ENGINE=default */\nSELECT * FROM SALES", routed.executionSql);
    }

    // Covers SqlRouteService#routeAndRewrite preserved-metadata precedence with engine hints ignored.
    @Test
    void shouldPreferYhTargetEngineOverDriverEngineHint() {
        SqlRouteService service = new SqlRouteService(registry(managerConfigs("default", "h2", true, "presto_local", "PRESTO", false)));
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse("/* YH_TARGET_ENGINE=presto_local */ -- engine=default\nSELECT * FROM SALES");

        RoutedSql routed = service.routeAndRewrite("SELECT * FROM SALES", parsed);

        assertEquals("presto_local", routed.datasourceName);
        assertEquals("PRESTO", routed.datasourceType);
        assertEquals("/* YH_TARGET_ENGINE=presto_local */\nSELECT * FROM SALES", routed.executionSql);
    }

    // Covers SqlRouteService#routeAndRewrite unknown-datasource fallback branch.
    @Test
    void shouldFallbackToDefaultDatasourceWhenTargetIsUnknown() {
        SqlRouteService service = new SqlRouteService(registry(managerConfigs("default", "h2", true)));
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse("/* YH_TARGET_ENGINE=missing */ SELECT * FROM SALES");

        RoutedSql routed = service.routeAndRewrite("SELECT * FROM SALES", parsed);

        assertEquals("default", routed.datasourceName);
        assertEquals("h2", routed.datasourceType);
        assertEquals("/* YH_TARGET_ENGINE=default */\nSELECT * FROM SALES", routed.executionSql);
    }

    // Covers SqlRouteService#routeAndRewrite adapter lookup for normalized datasource types.
    @Test
    void shouldUseDatasourceTypeLookupCaseInsensitively() {
        SqlRouteService service = new SqlRouteService(registry(managerConfigs("default", "PRESTO", true)));

        RoutedSql routed = service.routeAndRewrite("SELECT * FROM SALES", SqlCommentParser.parse("SELECT * FROM SALES"));

        assertEquals("PRESTO", routed.datasourceType);
        assertEquals("/* YH_TARGET_ENGINE=default */\nSELECT * FROM SALES", routed.executionSql);
    }

    // Covers SqlRouteService#routeAndRewrite explicit Trino adapter lookup.
    @Test
    void shouldRouteTrinoDatasourcesWithoutSqlRewrite() {
        SqlRouteService service = new SqlRouteService(registry(managerConfigs("trino_local", "TRINO", true)));

        RoutedSql routed = service.routeAndRewrite("SELECT * FROM NATION", SqlCommentParser.parse("SELECT * FROM NATION"));

        assertEquals("trino_local", routed.datasourceName);
        assertEquals("TRINO", routed.datasourceType);
        assertEquals("/* YH_TARGET_ENGINE=trino_local */\nSELECT * FROM NATION", routed.executionSql);
    }

    // Covers SqlRouteService#routeAndRewrite parsed-null fallback branch.
    @Test
    void shouldHandleNullParsedSqlByPassingThroughOriginalSql() {
        SqlRouteService service = new SqlRouteService(registry(managerConfigs("default", "h2", true)));

        RoutedSql routed = service.routeAndRewrite("SELECT * FROM SALES", null);

        assertEquals("default", routed.datasourceName);
        assertEquals("/* YH_TARGET_ENGINE=default */\nSELECT * FROM SALES", routed.executionSql);
    }

    private static ManagedDataSourceRegistry registry(List<ManagerConfigClient.ManagerDatasourceConfig> configs) {
        QueryProperties properties = new QueryProperties();
        properties.setManagerUrl(QueryTestFixtures.get("query.test.manager-url"));
        return new ManagedDataSourceRegistry(properties, new StubManagerConfigClient(configs));
    }

    private static List<ManagerConfigClient.ManagerDatasourceConfig> managerConfigs(Object... values) {
        java.util.ArrayList<ManagerConfigClient.ManagerDatasourceConfig> configs =
                new java.util.ArrayList<ManagerConfigClient.ManagerDatasourceConfig>();
        for (int i = 0; i < values.length; i += 3) {
            ManagerConfigClient.ManagerDatasourceConfig config = new ManagerConfigClient.ManagerDatasourceConfig();
            config.setName((String) values[i]);
            config.setType((String) values[i + 1]);
            config.setDriverClass(QueryTestFixtures.get("query.test.registry.shared.driver-class"));
            config.setJdbcUrl(QueryTestFixtures.get("query.test.registry.manager.jdbc-url-prefix") + values[i]);
            config.setUsername(QueryTestFixtures.get("query.test.registry.shared.username"));
            config.setPassword(QueryTestFixtures.get("query.test.registry.shared.password"));
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
