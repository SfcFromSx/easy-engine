package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkDataSource;
import com.smartbi.benchmark.jdbc.JdbcDriverRegistry;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.support.BenchmarkTestFixtures;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSourceControllerTest {

    private static final String UPDATE_JDBC_URL = BenchmarkTestFixtures.get("benchmark.test.datasource.update.jdbc-url");
    private static final String UPDATE_JDBC_USER = BenchmarkTestFixtures.get("benchmark.test.datasource.update.jdbc-user");
    private static final String UPDATE_JDBC_PASSWORD = BenchmarkTestFixtures.get("benchmark.test.datasource.update.jdbc-password");
    private static final String UPDATE_DRIVER_CLASS = BenchmarkTestFixtures.get("benchmark.test.datasource.update.driver-class");

    // Covers DataSourceController#update editable field propagation.
    @Test
    void shouldUpdateEditableDatasourceFields() {
        BenchmarkDataSourceRepository repository = mock(BenchmarkDataSourceRepository.class);
        BenchmarkJobRepository jobRepository = mock(BenchmarkJobRepository.class);
        BenchmarkQueryService queryService = mock(BenchmarkQueryService.class);
        JdbcDriverRegistry driverRegistry = mock(JdbcDriverRegistry.class);
        DataSourceController controller = new DataSourceController(repository, jobRepository, queryService, driverRegistry);

        BenchmarkDataSource existing = new BenchmarkDataSource();
        existing.setId(8L);
        existing.setName("old");
        BenchmarkDataSource payload = new BenchmarkDataSource();
        payload.setName("new");
        payload.setJdbcUrl(UPDATE_JDBC_URL);
        payload.setJdbcUser(UPDATE_JDBC_USER);
        payload.setJdbcPassword(UPDATE_JDBC_PASSWORD);
        payload.setDriverClass(UPDATE_DRIVER_CLASS);
        when(repository.findById(8L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        BenchmarkDataSource saved = controller.update(8L, payload);

        assertSame(existing, saved);
        assertEquals("new", existing.getName());
        assertEquals(UPDATE_JDBC_URL, existing.getJdbcUrl());
        assertEquals(UPDATE_JDBC_USER, existing.getJdbcUser());
        assertEquals(UPDATE_JDBC_PASSWORD, existing.getJdbcPassword());
        assertEquals(UPDATE_DRIVER_CLASS, existing.getDriverClass());
    }

    // Covers DataSourceController#remove guard when a datasource is still referenced by jobs.
    @Test
    void shouldRejectDatasourceRemovalWhenJobsStillReferenceIt() {
        BenchmarkDataSourceRepository repository = mock(BenchmarkDataSourceRepository.class);
        BenchmarkJobRepository jobRepository = mock(BenchmarkJobRepository.class);
        BenchmarkQueryService queryService = mock(BenchmarkQueryService.class);
        JdbcDriverRegistry driverRegistry = mock(JdbcDriverRegistry.class);
        DataSourceController controller = new DataSourceController(repository, jobRepository, queryService, driverRegistry);
        when(jobRepository.existsByDataSourceId(9L)).thenReturn(true);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> controller.remove(9L));

        assertEquals("该数据源仍被压测任务引用，无法删除。请先修改或删除相关任务。", error.getMessage());
    }

    // Covers DataSourceController#testConnection success and failure responses.
    @Test
    void shouldReturnConnectionProbeResults() throws Exception {
        BenchmarkDataSourceRepository repository = mock(BenchmarkDataSourceRepository.class);
        BenchmarkJobRepository jobRepository = mock(BenchmarkJobRepository.class);
        BenchmarkQueryService queryService = mock(BenchmarkQueryService.class);
        JdbcDriverRegistry driverRegistry = mock(JdbcDriverRegistry.class);
        DataSourceController controller = new DataSourceController(repository, jobRepository, queryService, driverRegistry);
        BenchmarkDataSource dataSource = new BenchmarkDataSource();
        Connection connection = mock(Connection.class);
        when(driverRegistry.openConnection(any(BenchmarkDataSource.class))).thenReturn(connection);

        assertEquals("SUCCESS", controller.testConnection(dataSource));
        verify(connection).close();

        when(driverRegistry.openConnection(any(BenchmarkDataSource.class))).thenThrow(new IllegalStateException("driver missing"));
        assertEquals("FAILED: driver missing", controller.testConnection(dataSource));
    }
}
