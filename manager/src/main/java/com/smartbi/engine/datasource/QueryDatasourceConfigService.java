package com.smartbi.engine.datasource;

import com.smartbi.engine.web.dto.QueryDatasourceConfigUpsertRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class QueryDatasourceConfigService {

    private final QueryDatasourceConfigRepository repository;

    public QueryDatasourceConfigService(QueryDatasourceConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<QueryDatasourceConfig> list() {
        return repository.findAll(Sort.by(
                Sort.Order.desc("isDefault"),
                Sort.Order.asc("name")
        ));
    }

    @Transactional
    public QueryDatasourceConfig create(QueryDatasourceConfigUpsertRequest request) {
        validate(request);
        if (repository.findByName(request.getName().trim()).isPresent()) {
            throw new IllegalStateException("Datasource name already exists: " + request.getName().trim());
        }

        QueryDatasourceConfig config = new QueryDatasourceConfig();
        apply(config, request);
        return saveWithDefaultHandling(config);
    }

    @Transactional
    public QueryDatasourceConfig update(long id, QueryDatasourceConfigUpsertRequest request) {
        validate(request);
        QueryDatasourceConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Datasource config not found: " + id));
        QueryDatasourceConfig duplicate = repository.findByName(request.getName().trim()).orElse(null);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new IllegalStateException("Datasource name already exists: " + request.getName().trim());
        }

        apply(config, request);
        return saveWithDefaultHandling(config);
    }

    @Transactional
    public void delete(long id) {
        QueryDatasourceConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Datasource config not found: " + id));
        boolean wasDefault = Boolean.TRUE.equals(config.getIsDefault());
        repository.delete(config);

        if (wasDefault) {
            List<QueryDatasourceConfig> remaining = list();
            if (!remaining.isEmpty()) {
                QueryDatasourceConfig promoted = remaining.get(0);
                promoted.setIsDefault(Boolean.TRUE);
                repository.save(promoted);
            }
        }
    }

    private QueryDatasourceConfig saveWithDefaultHandling(QueryDatasourceConfig config) {
        QueryDatasourceConfig saved = repository.save(config);
        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            for (QueryDatasourceConfig candidate : repository.findAll()) {
                if (!candidate.getId().equals(saved.getId()) && Boolean.TRUE.equals(candidate.getIsDefault())) {
                    candidate.setIsDefault(Boolean.FALSE);
                    repository.save(candidate);
                }
            }
        }
        return saved;
    }

    private void apply(QueryDatasourceConfig config, QueryDatasourceConfigUpsertRequest request) {
        config.setName(request.getName().trim());
        config.setType(request.getType().trim());
        config.setDriverClass(request.getDriverClass().trim());
        config.setJdbcUrl(request.getJdbcUrl().trim());
        config.setUsername(trimToNull(request.getUsername()));
        config.setPassword(request.getPassword());
        config.setMaxPoolSize(request.getMaxPoolSize() == null ? Integer.valueOf(4) : request.getMaxPoolSize());
        config.setMinIdle(request.getMinIdle() == null ? Integer.valueOf(1) : request.getMinIdle());
        config.setConnectionTimeoutMs(request.getConnectionTimeoutMs() == null
                ? Long.valueOf(10000L)
                : request.getConnectionTimeoutMs());
        config.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
    }

    private void validate(QueryDatasourceConfigUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        requireText(request.getName(), "name");
        requireText(request.getType(), "type");
        requireText(request.getDriverClass(), "driverClass");
        requireText(request.getJdbcUrl(), "jdbcUrl");
        if (request.getMaxPoolSize() != null && request.getMaxPoolSize().intValue() < 1) {
            throw new IllegalArgumentException("maxPoolSize must be >= 1");
        }
        if (request.getMinIdle() != null && request.getMinIdle().intValue() < 0) {
            throw new IllegalArgumentException("minIdle must be >= 0");
        }
        if (request.getConnectionTimeoutMs() != null && request.getConnectionTimeoutMs().longValue() < 1000L) {
            throw new IllegalArgumentException("connectionTimeoutMs must be >= 1000");
        }
    }

    private static void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
