package com.smartbi.engine.datasource;

import com.smartbi.engine.web.dto.QueryDatasourceConfigUpsertRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueryDatasourceConfigServiceTest {

    private final QueryDatasourceConfigRepository repository = Mockito.mock(QueryDatasourceConfigRepository.class);
    private final QueryDatasourceConfigService service = new QueryDatasourceConfigService(repository);

    @Test
    // Covers QueryDatasourceConfigService#get.
    void getDelegatesToRepository() {
        QueryDatasourceConfig config = new QueryDatasourceConfig();
        when(repository.findById(7L)).thenReturn(Optional.of(config));

        Optional<QueryDatasourceConfig> result = service.get(7L);

        assertSame(config, result.orElseThrow(AssertionError::new));
    }

    @Test
    // Covers QueryDatasourceConfigService#list.
    void listSortsByDefaultAndName() {
        when(repository.findAll(any(Sort.class))).thenReturn(Collections.emptyList());

        service.list();

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findAll(sortCaptor.capture());
        assertEquals("isDefault: DESC,name: ASC", sortCaptor.getValue().toString());
    }

    @Test
    // Covers QueryDatasourceConfigService#create and QueryDatasourceConfigService#apply.
    void createTrimsFieldsAndAppliesDefaults() {
        QueryDatasourceConfig existingDefault = new QueryDatasourceConfig();
        ReflectionTestUtils.setField(existingDefault, "id", 2L);
        existingDefault.setIsDefault(Boolean.TRUE);

        when(repository.findByName("analytics")).thenReturn(Optional.empty());
        when(repository.save(any(QueryDatasourceConfig.class))).thenAnswer(invocation -> {
            QueryDatasourceConfig saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", 9L);
            }
            return saved;
        });
        when(repository.findAll()).thenReturn(Collections.singletonList(existingDefault));

        QueryDatasourceConfig created = service.create(request(" analytics ", Boolean.TRUE));

        assertEquals("analytics", created.getName());
        assertEquals("mysql", created.getType());
        assertEquals("com.mysql.jdbc.Driver", created.getDriverClass());
        assertEquals("jdbc:mysql://localhost/test", created.getJdbcUrl());
        assertEquals("svc_user", created.getUsername());
        assertNull(created.getPassword());
        assertEquals(Integer.valueOf(4), created.getMaxPoolSize());
        assertEquals(Integer.valueOf(1), created.getMinIdle());
        assertEquals(Long.valueOf(10000L), created.getConnectionTimeoutMs());
        assertEquals(Boolean.TRUE, created.getIsDefault());
        assertEquals(Boolean.FALSE, existingDefault.getIsDefault());
    }

    @Test
    // Covers QueryDatasourceConfigService#create duplicate-name rejection.
    void createRejectsDuplicateTrimmedName() {
        when(repository.findByName("analytics")).thenReturn(Optional.of(new QueryDatasourceConfig()));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.create(request(" analytics ", Boolean.FALSE)));

        assertEquals("Datasource name already exists: analytics", error.getMessage());
    }

    @Test
    // Covers QueryDatasourceConfigService#update validation and duplicate guarding.
    void updateRejectsDuplicateOwnedByAnotherRecord() {
        QueryDatasourceConfig existing = new QueryDatasourceConfig();
        ReflectionTestUtils.setField(existing, "id", 3L);
        QueryDatasourceConfig duplicate = new QueryDatasourceConfig();
        ReflectionTestUtils.setField(duplicate, "id", 5L);

        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.findByName("analytics")).thenReturn(Optional.of(duplicate));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.update(3L, request("analytics", Boolean.FALSE)));

        assertEquals("Datasource name already exists: analytics", error.getMessage());
    }

    @Test
    // Covers QueryDatasourceConfigService#validate numeric boundaries.
    void createRejectsInvalidNumericBoundaries() {
        QueryDatasourceConfigUpsertRequest maxPoolRequest = request("analytics", Boolean.FALSE);
        maxPoolRequest.setMaxPoolSize(0);
        assertEquals("maxPoolSize must be >= 1",
                assertThrows(IllegalArgumentException.class, () -> service.create(maxPoolRequest)).getMessage());

        QueryDatasourceConfigUpsertRequest minIdleRequest = request("analytics", Boolean.FALSE);
        minIdleRequest.setMinIdle(-1);
        assertEquals("minIdle must be >= 0",
                assertThrows(IllegalArgumentException.class, () -> service.create(minIdleRequest)).getMessage());

        QueryDatasourceConfigUpsertRequest timeoutRequest = request("analytics", Boolean.FALSE);
        timeoutRequest.setConnectionTimeoutMs(999L);
        assertEquals("connectionTimeoutMs must be >= 1000",
                assertThrows(IllegalArgumentException.class, () -> service.create(timeoutRequest)).getMessage());
    }

    @Test
    // Covers QueryDatasourceConfigService#delete default promotion.
    void deletePromotesNextDatasourceWhenDefaultIsRemoved() {
        QueryDatasourceConfig currentDefault = new QueryDatasourceConfig();
        ReflectionTestUtils.setField(currentDefault, "id", 11L);
        currentDefault.setIsDefault(Boolean.TRUE);

        QueryDatasourceConfig promoted = new QueryDatasourceConfig();
        ReflectionTestUtils.setField(promoted, "id", 12L);
        promoted.setName("backup");
        promoted.setIsDefault(Boolean.FALSE);

        when(repository.findById(11L)).thenReturn(Optional.of(currentDefault));
        when(repository.findAll(any(Sort.class))).thenReturn(Collections.singletonList(promoted));
        when(repository.save(any(QueryDatasourceConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(11L);

        verify(repository).delete(currentDefault);
        assertEquals(Boolean.TRUE, promoted.getIsDefault());
    }

    private static QueryDatasourceConfigUpsertRequest request(String name, Boolean isDefault) {
        QueryDatasourceConfigUpsertRequest request = new QueryDatasourceConfigUpsertRequest();
        request.setName(name);
        request.setType(" mysql ");
        request.setDriverClass(" com.mysql.jdbc.Driver ");
        request.setJdbcUrl(" jdbc:mysql://localhost/test ");
        request.setUsername(" svc_user ");
        request.setPassword(null);
        request.setIsDefault(isDefault);
        return request;
    }
}
