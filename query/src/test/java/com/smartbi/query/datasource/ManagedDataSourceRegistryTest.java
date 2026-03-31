package com.smartbi.query.datasource;

import com.smartbi.query.config.ManagerConfigClient;
import com.smartbi.query.config.QueryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedDataSourceRegistryTest {

    // Covers ManagedDataSourceRegistry#loadDefinitions remote-manager happy path.
    @Test
    void shouldPreferManagerDatasourceConfigsWhenAvailable() {
        QueryProperties queryProperties = fallbackProperties();
        ManagedDataSourceRegistry registry = new ManagedDataSourceRegistry(
                queryProperties,
                new StubManagerConfigClient(Arrays.asList(managerConfig("remote_default", "h2", true),
                        managerConfig("remote_presto", "presto", false)))
        );

        assertEquals("remote_default", registry.getDefaultName());
        assertEquals("jdbc:h2:mem:remote_default", registry.getDefinition("remote_default").getJdbcUrl());
        assertEquals("presto", registry.getDefinition("remote_presto").getType());
        assertEquals("remote_default", registry.getDefinition("missing").getName());
    }

    // Covers ManagedDataSourceRegistry#loadDefinitions manager-failure fallback to static config.
    @Test
    void shouldFallbackToStaticDatasourceConfigsWhenManagerUnavailable() {
        QueryProperties queryProperties = fallbackProperties();
        ManagedDataSourceRegistry registry = new ManagedDataSourceRegistry(
                queryProperties,
                new StubManagerConfigClient(new IllegalStateException("manager offline"))
        );

        assertEquals("default", registry.getDefaultName());
        DataSourceDefinition fallback = registry.getDefinition("presto_local");
        assertNotNull(fallback);
        assertEquals("presto_local", fallback.getName());
        assertEquals("jdbc:h2:mem:fallback_presto", fallback.getJdbcUrl());
    }

    // Covers ManagedDataSourceRegistry#remoteBootstrap blank/null config filtering and explicit-default precedence.
    @Test
    void shouldIgnoreBlankManagerConfigsAndPreferExplicitDefaultFlag() {
        QueryProperties queryProperties = fallbackProperties();
        ManagedDataSourceRegistry registry = new ManagedDataSourceRegistry(
                queryProperties,
                new StubManagerConfigClient(Arrays.asList(
                        null,
                        managerConfig("  ", "h2", false),
                        managerConfig("remote_default", "h2", false),
                        managerConfig("remote_explicit", "presto", true)))
        );

        assertEquals("remote_explicit", registry.getDefaultName());
        assertEquals("remote_default", registry.getDefinition("remote_default").getName());
        assertEquals("remote_explicit", registry.getDefinition("missing").getName());
    }

    // Covers ManagedDataSourceRegistry#loadDefinitions fallback when manager returns no usable configs.
    @Test
    void shouldFallbackWhenManagerReturnsOnlyBlankConfigs() {
        QueryProperties queryProperties = fallbackProperties();
        ManagedDataSourceRegistry registry = new ManagedDataSourceRegistry(
                queryProperties,
                new StubManagerConfigClient(Arrays.asList(managerConfig(" ", "h2", true)))
        );

        assertEquals("default", registry.getDefaultName());
        assertEquals("default", registry.getDefinition("missing").getName());
    }

    // Covers ManagedDataSourceRegistry#fallbackBootstrap named-datasource key fallback branch.
    @Test
    void shouldUseMapKeyWhenFallbackNamedDatasourceNameIsBlank() {
        QueryProperties queryProperties = fallbackProperties();
        QueryProperties.NamedDatasource unnamed = new QueryProperties.NamedDatasource();
        unnamed.setName(" ");
        unnamed.setType("presto");
        unnamed.setDriverClass("org.h2.Driver");
        unnamed.setJdbcUrl("jdbc:h2:mem:keyed_named");
        unnamed.setUsername("sa");
        unnamed.setPassword("");
        queryProperties.getDatasource().getNamed().put("keyed_name", unnamed);

        ManagedDataSourceRegistry registry = new ManagedDataSourceRegistry(
                queryProperties,
                new StubManagerConfigClient(Collections.<ManagerConfigClient.ManagerDatasourceConfig>emptyList())
        );

        assertEquals("keyed_name", registry.getDefinition("keyed_name").getName());
    }

    // Covers ManagedDataSourceRegistry#getConnection and #getOrCreate datasource caching branches.
    @Test
    void shouldReuseDataSourcePoolsAcrossConnections() throws Exception {
        QueryProperties queryProperties = fallbackProperties();
        ManagedDataSourceRegistry registry = new ManagedDataSourceRegistry(
                queryProperties,
                new StubManagerConfigClient(Collections.<ManagerConfigClient.ManagerDatasourceConfig>emptyList())
        );

        Connection first = registry.getConnection("default");
        first.close();
        Connection second = registry.getConnection("default");
        second.close();

        Map<?, ?> dataSources = dataSources(registry);
        assertEquals(1, dataSources.size());
        assertSame(dataSources.get("default"), dataSources.get("default"));

        registry.close();
    }

    // Covers ManagedDataSourceRegistry#getOrCreate missing-driver error branch.
    @Test
    void shouldFailWhenDriverClassCannotBeLoaded() {
        QueryProperties queryProperties = fallbackProperties();
        queryProperties.getDatasource().getDefault().setDriverClass("missing.Driver");
        ManagedDataSourceRegistry registry = new ManagedDataSourceRegistry(
                queryProperties,
                new StubManagerConfigClient(Collections.<ManagerConfigClient.ManagerDatasourceConfig>emptyList())
        );

        java.sql.SQLException exception = assertThrows(java.sql.SQLException.class, () -> registry.getConnection("default"));

        assertTrue(exception.getMessage().contains("Unable to load driver class"));
    }

    // Covers ManagedDataSourceRegistry#close pool shutdown branch.
    @Test
    void shouldCloseManagedDataSources() throws Exception {
        QueryProperties queryProperties = fallbackProperties();
        ManagedDataSourceRegistry registry = new ManagedDataSourceRegistry(
                queryProperties,
                new StubManagerConfigClient(Collections.<ManagerConfigClient.ManagerDatasourceConfig>emptyList())
        );

        Connection connection = registry.getConnection("default");
        connection.close();

        Map<?, ?> dataSources = dataSources(registry);
        DataSource dataSource = (DataSource) dataSources.get("default");
        assertNotNull(dataSource);

        registry.close();

        assertTrue(((com.zaxxer.hikari.HikariDataSource) dataSource).isClosed());
    }

    private static QueryProperties fallbackProperties() {
        QueryProperties queryProperties = new QueryProperties();
        queryProperties.setManagerUrl("http://localhost:8090");

        QueryProperties.NamedDatasource defaultDatasource = queryProperties.getDatasource().getDefault();
        defaultDatasource.setName("default");
        defaultDatasource.setType("h2");
        defaultDatasource.setDriverClass("org.h2.Driver");
        defaultDatasource.setJdbcUrl("jdbc:h2:mem:fallback_default");
        defaultDatasource.setUsername("sa");
        defaultDatasource.setPassword("");

        QueryProperties.NamedDatasource prestoDatasource = new QueryProperties.NamedDatasource();
        prestoDatasource.setName("presto_local");
        prestoDatasource.setType("presto");
        prestoDatasource.setDriverClass("org.h2.Driver");
        prestoDatasource.setJdbcUrl("jdbc:h2:mem:fallback_presto");
        prestoDatasource.setUsername("sa");
        prestoDatasource.setPassword("");
        queryProperties.getDatasource().getNamed().put("presto_local", prestoDatasource);

        return queryProperties;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, DataSource> dataSources(ManagedDataSourceRegistry registry) {
        return (Map<String, DataSource>) ReflectionTestUtils.getField(registry, "dataSources");
    }

    private static ManagerConfigClient.ManagerDatasourceConfig managerConfig(String name,
                                                                            String type,
                                                                            boolean isDefault) {
        ManagerConfigClient.ManagerDatasourceConfig config = new ManagerConfigClient.ManagerDatasourceConfig();
        config.setName(name);
        config.setType(type);
        config.setDriverClass("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:mem:" + name);
        config.setUsername("sa");
        config.setPassword("");
        config.setMaxPoolSize(Integer.valueOf(4));
        config.setMinIdle(Integer.valueOf(1));
        config.setConnectionTimeoutMs(Long.valueOf(10000L));
        config.setIsDefault(Boolean.valueOf(isDefault));
        return config;
    }

    private static class StubManagerConfigClient extends ManagerConfigClient {
        private final List<ManagerDatasourceConfig> configs;
        private final RuntimeException failure;

        private StubManagerConfigClient(List<ManagerDatasourceConfig> configs) {
            super(new RestTemplateBuilder(), new QueryProperties());
            this.configs = configs;
            this.failure = null;
        }

        private StubManagerConfigClient(RuntimeException failure) {
            super(new RestTemplateBuilder(), new QueryProperties());
            this.configs = Collections.emptyList();
            this.failure = failure;
        }

        @Override
        public List<ManagerDatasourceConfig> fetchDatasourceConfigs() {
            if (failure != null) {
                throw failure;
            }
            return configs;
        }
    }
}
