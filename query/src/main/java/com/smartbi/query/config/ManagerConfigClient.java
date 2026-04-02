package com.smartbi.query.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class ManagerConfigClient {

    private static final Logger log = LoggerFactory.getLogger(ManagerConfigClient.class);

    private final RestTemplate restTemplate;
    private final QueryProperties queryProperties;

    public ManagerConfigClient(RestTemplateBuilder restTemplateBuilder,
                               QueryProperties queryProperties) {
        this.restTemplate = restTemplateBuilder.build();
        this.queryProperties = queryProperties;
    }

    public List<ManagerDatasourceConfig> fetchDatasourceConfigs() {
        ManagerRoutingContext routingContext = fetchRoutingContext();
        return routingContext == null || routingContext.getDatasources() == null
                ? Collections.<ManagerDatasourceConfig>emptyList()
                : routingContext.getDatasources();
    }

    public ManagerRoutingContext fetchRoutingContext() {
        String managerUrl = queryProperties.getManagerUrl();
        if (!StringUtils.hasText(managerUrl)) {
            return new ManagerRoutingContext();
        }
        String baseUrl = managerUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + "/api/v1/query-routing-context";
        try {
            ResponseEntity<ManagerRoutingContext> response = restTemplate.getForEntity(
                    url,
                    ManagerRoutingContext.class);
            ManagerRoutingContext body = response.getBody();
            if (body == null) {
                log.warn("Manager routing context endpoint returned no payload: {}", url);
                return new ManagerRoutingContext();
            }
            if (body.getDatasources() == null) {
                body.setDatasources(Collections.<ManagerDatasourceConfig>emptyList());
            }
            if (body.getAccelerationRules() == null) {
                body.setAccelerationRules(Collections.<ManagerAccelerationRule>emptyList());
            }
            return body;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to load routing context from " + url, ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ManagerDatasourceConfig {
        private String name;
        private String type;
        private String driverClass;
        private String jdbcUrl;
        private String username;
        private String password;
        private Integer maxPoolSize;
        private Integer minIdle;
        private Long connectionTimeoutMs;
        private Boolean isDefault;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDriverClass() {
            return driverClass;
        }

        public void setDriverClass(String driverClass) {
            this.driverClass = driverClass;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Integer getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(Integer maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public Integer getMinIdle() {
            return minIdle;
        }

        public void setMinIdle(Integer minIdle) {
            this.minIdle = minIdle;
        }

        public Long getConnectionTimeoutMs() {
            return connectionTimeoutMs;
        }

        public void setConnectionTimeoutMs(Long connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }

        public Boolean getIsDefault() {
            return isDefault;
        }

        public void setIsDefault(Boolean isDefault) {
            this.isDefault = isDefault;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ManagerAccelerationRule {
        private String name;
        private String schemaName;
        private String tableName;
        private String refreshSql;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public void setSchemaName(String schemaName) {
            this.schemaName = schemaName;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getRefreshSql() {
            return refreshSql;
        }

        public void setRefreshSql(String refreshSql) {
            this.refreshSql = refreshSql;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ManagerRoutingContext {
        private List<ManagerDatasourceConfig> datasources = Collections.emptyList();
        private List<ManagerAccelerationRule> accelerationRules = Collections.emptyList();

        public List<ManagerDatasourceConfig> getDatasources() {
            return datasources;
        }

        public void setDatasources(List<ManagerDatasourceConfig> datasources) {
            this.datasources = datasources;
        }

        public List<ManagerAccelerationRule> getAccelerationRules() {
            return accelerationRules;
        }

        public void setAccelerationRules(List<ManagerAccelerationRule> accelerationRules) {
            this.accelerationRules = accelerationRules;
        }
    }
}
