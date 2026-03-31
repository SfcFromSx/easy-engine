package com.smartbi.engine.accel;

import com.smartbi.engine.domain.AccelerationSource;
import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationTable;
import com.smartbi.engine.domain.SqlPatternStats;
import com.smartbi.engine.repo.AccelerationTableRepository;
import com.smartbi.engine.repo.SqlPatternStatsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccelerationServiceTest {

    private final AccelerationTableRepository accelerationTableRepository = Mockito.mock(AccelerationTableRepository.class);
    private final SqlPatternStatsRepository sqlPatternStatsRepository = Mockito.mock(SqlPatternStatsRepository.class);
    private final JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);

    private final AccelerationService service = new AccelerationService(
            accelerationTableRepository,
            sqlPatternStatsRepository,
            jdbcTemplate
    );

    @Test
    // Covers AccelerationService#createManual duplicate validation.
    void createManualRejectsDuplicateSchemaAndName() {
        when(accelerationTableRepository.existsByNameIgnoreCaseAndSchemaNameIgnoreCase("rollup_sales", "public"))
                .thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createManual("rollup_sales", "public", "CREATE TABLE x(id int)", null, null)
        );

        assertEquals("acceleration table already exists for schema/name", error.getMessage());
    }

    @Test
    // Covers AccelerationService#updateManual.
    void updateManualKeepsExistingStatusAndSource() {
        AccelerationTable existing = new AccelerationTable();
        existing.setName("old_name");
        existing.setSchemaName("public");
        existing.setDdlText("CREATE TABLE old_name(id int)");
        existing.setStatus(AccelerationStatus.ACTIVE);
        existing.setSource(AccelerationSource.RECOMMENDED);

        when(accelerationTableRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(accelerationTableRepository.existsByNameIgnoreCaseAndSchemaNameIgnoreCaseAndIdNot("new_name", "analytics", 7L))
                .thenReturn(false);
        when(accelerationTableRepository.save(any(AccelerationTable.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccelerationTable updated = service.updateManual(
                7L,
                "new_name",
                "analytics",
                "CREATE TABLE new_name(id int)",
                "INSERT INTO new_name SELECT 1",
                "0 0 * * * ?"
        );

        assertEquals("new_name", updated.getName());
        assertEquals("analytics", updated.getSchemaName());
        assertEquals(AccelerationStatus.ACTIVE, updated.getStatus());
        assertEquals(AccelerationSource.RECOMMENDED, updated.getSource());
    }

    @Test
    // Covers AccelerationService#delete.
    void deleteRemovesExistingEntity() {
        AccelerationTable existing = new AccelerationTable();
        when(accelerationTableRepository.findById(9L)).thenReturn(Optional.of(existing));

        service.delete(9L);

        verify(accelerationTableRepository).delete(existing);
    }

    @Test
    // Covers AccelerationService#createFromPattern.
    void createFromPatternBuildsRecommendedDraftWithSanitizedName() {
        SqlPatternStats stats = new SqlPatternStats();
        ReflectionTestUtils.setField(stats, "id", 5L);
        stats.setSqlFingerprint("fp-1");
        stats.setExecutionCount(12);
        stats.setCleanSqlSample("SELECT count(*) FROM sales");

        when(sqlPatternStatsRepository.findById(5L)).thenReturn(Optional.of(stats));
        when(accelerationTableRepository.existsByNameIgnoreCaseAndSchemaNameIgnoreCase("rollup_sales_2026", "analytics"))
                .thenReturn(false);
        when(accelerationTableRepository.save(any(AccelerationTable.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccelerationTable created = service.createFromPattern(5L, "Rollup Sales 2026", "analytics");

        assertEquals("rollup_sales_2026", created.getName());
        assertEquals("analytics", created.getSchemaName());
        assertEquals(AccelerationStatus.DRAFT, created.getStatus());
        assertEquals(AccelerationSource.RECOMMENDED, created.getSource());
        assertEquals("From pattern fp-1 count=12", created.getRecommendationNote());
        assertEquals("0 30 2 * * ?", created.getCronExpr());
        org.junit.jupiter.api.Assertions.assertTrue(created.getDdlText().contains("WITH NO DATA;"));
        org.junit.jupiter.api.Assertions.assertTrue(created.getRefreshSql().contains("SELECT count(*) FROM sales;"));
    }

    @Test
    // Covers AccelerationService#list filtered path.
    void listDelegatesToSpecificationQuery() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(accelerationTableRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        service.list(pageable, "sales", "ACTIVE", "analytics", "RECOMMENDED");

        verify(accelerationTableRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    // Covers AccelerationService#topPatterns filtered and unfiltered branches.
    void topPatternsDelegatesToMatchingRepositoryQuery() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(sqlPatternStatsRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        service.topPatterns(pageable, null, null, null);
        service.topPatterns(pageable, " fp-1 ", "sales", 10L);

        verify(sqlPatternStatsRepository, Mockito.times(2)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    // Covers AccelerationService#updateStatus ACTIVE branch.
    void updateStatusCreatesSchemaAndRunsRefreshSqlWhenActivating() {
        AccelerationTable table = new AccelerationTable();
        ReflectionTestUtils.setField(table, "id", 7L);
        table.setName("mv_sales");
        table.setSchemaName("analytics");
        table.setDdlText("CREATE TABLE analytics.mv_sales(id int)");
        table.setRefreshSql("INSERT INTO analytics.mv_sales SELECT 1");
        table.setStatus(AccelerationStatus.DRAFT);

        when(accelerationTableRepository.findById(7L)).thenReturn(Optional.of(table));
        when(accelerationTableRepository.save(any(AccelerationTable.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccelerationTable updated = service.updateStatus(7L, AccelerationStatus.ACTIVE);

        assertEquals(AccelerationStatus.ACTIVE, updated.getStatus());
        verify(jdbcTemplate).execute("CREATE SCHEMA IF NOT EXISTS \"analytics\"");
        verify(jdbcTemplate).execute("CREATE TABLE analytics.mv_sales(id int)");
        verify(jdbcTemplate).execute("INSERT INTO analytics.mv_sales SELECT 1");
    }
}
