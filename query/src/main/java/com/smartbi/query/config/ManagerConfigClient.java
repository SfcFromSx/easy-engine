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
        String managerUrl = queryProperties.getManagerUrl();
        if (!StringUtils.hasText(managerUrl)) {
            return Collections.emptyList();
        }
        String baseUrl = managerUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + "/api/v1/query-datasources";
        try {
            ResponseEntity<ManagerDatasourceConfig[]> response = restTemplate.getForEntity(
                    url,
                    ManagerDatasourceConfig[].class);
            ManagerDatasourceConfig[] body = response.getBody();
            if (body == null || body.length == 0) {
                log.warn("Manager datasource config endpoint returned no rows: {}", url);
                return Collections.emptyList();
            }
            return Arrays.asList(body);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to load datasource configs from " + url, ex);
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
}
