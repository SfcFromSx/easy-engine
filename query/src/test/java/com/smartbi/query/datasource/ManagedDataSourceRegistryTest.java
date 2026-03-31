package com.smartbi.query.datasource;

import com.smartbi.query.config.ManagerConfigClient;
import com.smartbi.query.config.QueryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ManagedDataSourceRegistryTest {

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
