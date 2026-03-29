package com.smartbi.engine.accel;

import com.smartbi.engine.domain.AccelerationSource;
import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationTable;
import com.smartbi.engine.repo.AccelerationTableRepository;
import com.smartbi.engine.repo.SqlPatternStatsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    void deleteRemovesExistingEntity() {
        AccelerationTable existing = new AccelerationTable();
        when(accelerationTableRepository.findById(9L)).thenReturn(Optional.of(existing));

        service.delete(9L);

        verify(accelerationTableRepository).delete(existing);
    }
}
