package com.smartbi.query.datasource;

import com.smartbi.query.config.ManagerConfigClient;
import com.smartbi.query.config.QueryProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ManagedDataSourceRegistry implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ManagedDataSourceRegistry.class);

    private final Map<String, DataSourceDefinition> definitions = new LinkedHashMap<String, DataSourceDefinition>();
    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<String, DataSource>();
    private final String defaultName;

    public ManagedDataSourceRegistry(QueryProperties queryProperties,
                                     ManagerConfigClient managerConfigClient) {
        RegistryBootstrap bootstrap = loadDefinitions(queryProperties, managerConfigClient);
        this.defaultName = bootstrap.defaultName;
        this.definitions.putAll(bootstrap.definitions);
    }

    public String getDefaultName() {
        return defaultName;
    }

    public DataSourceDefinition getDefinition(String name) {
        DataSourceDefinition definition = definitions.get(name);
        if (definition == null) {
            definition = definitions.get(defaultName);
        }
        return definition;
    }

    public Connection getConnection(String name) throws SQLException {
        DataSource dataSource = getOrCreate(name);
        return dataSource.getConnection();
    }

    private DataSource getOrCreate(String requestedName) throws SQLException {
        String name = definitions.containsKey(requestedName) ? requestedName : defaultName;
        DataSource existing = dataSources.get(name);
        if (existing != null) {
            return existing;
        }
        synchronized (dataSources) {
            existing = dataSources.get(name);
            if (existing != null) {
                return existing;
            }
            DataSourceDefinition definition = definitions.get(name);
            if (definition == null) {
                throw new SQLException("No datasource definition found for " + name);
            }
            HikariConfig config = new HikariConfig();
            try {
                Class.forName(definition.getDriverClass());
            } catch (ClassNotFoundException ex) {
                throw new SQLException("Unable to load driver class " + definition.getDriverClass(), ex);
            }
            config.setDriverClassName(definition.getDriverClass());
            config.setJdbcUrl(definition.getJdbcUrl());
            config.setUsername(definition.getUsername());
            config.setPassword(definition.getPassword());
            config.setMaximumPoolSize(Math.max(1, definition.getMaxPoolSize()));
            config.setMinimumIdle(Math.max(0, definition.getMinIdle()));
            config.setConnectionTimeout(Math.max(1000L, definition.getConnectionTimeoutMs()));
            config.setPoolName("engine-query-" + name);
            HikariDataSource dataSource = new HikariDataSource(config);
            dataSources.put(name, dataSource);
            log.info("Initialized query datasource [{}] type={}", name, definition.getType());
            return dataSource;
        }
    }

    private static DataSourceDefinition toDefinition(String name, QueryProperties.NamedDatasource source) {
        return new DataSourceDefinition(
                name,
                source.getType(),
                source.getDriverClass(),
                source.getJdbcUrl(),
                source.getUsername(),
                source.getPassword(),
                source.getMaxPoolSize(),
                source.getMinIdle(),
                source.getConnectionTimeoutMs()
        );
    }

    private RegistryBootstrap loadDefinitions(QueryProperties queryProperties,
                                              ManagerConfigClient managerConfigClient) {
        try {
            List<ManagerConfigClient.ManagerDatasourceConfig> remoteConfigs = managerConfigClient.fetchDatasourceConfigs();
            RegistryBootstrap remoteBootstrap = remoteBootstrap(remoteConfigs);
            if (remoteBootstrap != null) {
                log.info("Loaded {} datasource configs from manager {}", Integer.valueOf(remoteBootstrap.definitions.size()),
                        queryProperties.getManagerUrl());
                return remoteBootstrap;
            }
            log.warn("Manager returned no usable datasource configs, falling back to static query config");
        } catch (Exception ex) {
            log.warn("Manager datasource config fetch failed, fallback to static query config: {}", ex.getMessage());
        }
        return fallbackBootstrap(queryProperties);
    }

    private RegistryBootstrap remoteBootstrap(List<ManagerConfigClient.ManagerDatasourceConfig> configs) {
        List<ManagerConfigClient.ManagerDatasourceConfig> safeConfigs = configs == null
                ? Collections.<ManagerConfigClient.ManagerDatasourceConfig>emptyList()
                : configs;
        Map<String, DataSourceDefinition> resolved = new LinkedHashMap<String, DataSourceDefinition>();
        String resolvedDefaultName = null;
        for (ManagerConfigClient.ManagerDatasourceConfig config : safeConfigs) {
            if (config == null) {
                continue;
            }
            String name = StringUtils.hasText(config.getName()) ? config.getName().trim() : null;
            if (!StringUtils.hasText(name)) {
                continue;
            }
            resolved.put(name, toDefinition(name, config));
            if (resolvedDefaultName == null || Boolean.TRUE.equals(config.getIsDefault())) {
                resolvedDefaultName = name;
            }
        }
        if (resolved.isEmpty()) {
            return null;
        }
        return new RegistryBootstrap(resolved, resolvedDefaultName);
    }

    private RegistryBootstrap fallbackBootstrap(QueryProperties queryProperties) {
        Map<String, DataSourceDefinition> resolved = new LinkedHashMap<String, DataSourceDefinition>();
        QueryProperties.NamedDatasource defaultDatasource = queryProperties.getDatasource().getDefault();
        String resolvedDefaultName = StringUtils.hasText(defaultDatasource.getName()) ? defaultDatasource.getName() : "default";
        resolved.put(resolvedDefaultName, toDefinition(resolvedDefaultName, defaultDatasource));

        for (Map.Entry<String, QueryProperties.NamedDatasource> entry : queryProperties.getDatasource().getNamed().entrySet()) {
            String name = StringUtils.hasText(entry.getValue().getName()) ? entry.getValue().getName() : entry.getKey();
            resolved.put(name, toDefinition(name, entry.getValue()));
        }
        return new RegistryBootstrap(resolved, resolvedDefaultName);
    }

    private static DataSourceDefinition toDefinition(String name, ManagerConfigClient.ManagerDatasourceConfig source) {
        return new DataSourceDefinition(
                name,
                source.getType(),
                source.getDriverClass(),
                source.getJdbcUrl(),
                source.getUsername(),
                source.getPassword(),
                source.getMaxPoolSize() == null ? 4 : source.getMaxPoolSize().intValue(),
                source.getMinIdle() == null ? 1 : source.getMinIdle().intValue(),
                source.getConnectionTimeoutMs() == null ? 10000L : source.getConnectionTimeoutMs().longValue()
        );
    }

    private static class RegistryBootstrap {
        private final Map<String, DataSourceDefinition> definitions;
        private final String defaultName;

        private RegistryBootstrap(Map<String, DataSourceDefinition> definitions, String defaultName) {
            this.definitions = definitions;
            this.defaultName = defaultName;
        }
    }

    @Override
    public void close() {
        for (DataSource dataSource : dataSources.values()) {
            if (dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
            }
        }
        dataSources.clear();
    }
}
