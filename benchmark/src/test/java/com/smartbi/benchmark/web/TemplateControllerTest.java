package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.SqlTemplate;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import com.smartbi.benchmark.sql.SqlLibImportService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateControllerTest {

    // Covers TemplateController#list SQL Lib search delegation with optional filters.
    @Test
    void shouldUseSearchQueryWhenKeywordIsProvided() {
        SqlTemplateRepository repository = mock(SqlTemplateRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        SqlLibImportService importService = mock(SqlLibImportService.class);
        TemplateController controller = new TemplateController(repository, itemRepository, importService);
        Page<SqlTemplate> expectedPage = new PageImpl<SqlTemplate>(Collections.singletonList(new SqlTemplate()));
        when(repository.searchLibrary(eq("prepared"), eq("PREPARED_STATEMENT"), eq("batch.csv"), isNull(), isNull(),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(expectedPage);

        Page<SqlTemplate> page = controller.list(0, 10, "prepared", "PREPARED_STATEMENT", "batch.csv", null, null);

        assertSame(expectedPage, page);
        verify(repository).searchLibrary(eq("prepared"), eq("PREPARED_STATEMENT"), eq("batch.csv"), isNull(), isNull(),
                any(org.springframework.data.domain.Pageable.class));
    }

    // Covers TemplateController#create, #get, and #update editable field propagation.
    @Test
    void shouldCreateGetAndUpdateSqlLibFields() {
        SqlTemplateRepository repository = mock(SqlTemplateRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        SqlLibImportService importService = mock(SqlLibImportService.class);
        TemplateController controller = new TemplateController(repository, itemRepository, importService);
        SqlTemplate existing = new SqlTemplate();
        existing.setId(4L);
        existing.setName("old");
        when(repository.findById(4L)).thenReturn(Optional.of(existing));
        when(repository.save(any(SqlTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SqlTemplate createPayload = new SqlTemplate();
        createPayload.setName(" created ");
        createPayload.setSqlText(" SELECT 1 ");
        createPayload.setWeight(0);
        SqlTemplate created = controller.create(createPayload);

        assertEquals("created", created.getName());
        assertEquals("SELECT 1", created.getSqlText());
        assertEquals(1, created.getWeight());
        assertEquals("STATEMENT", created.getExecutionMode());
        assertSame(existing, controller.get(4L));

        SqlTemplate payload = new SqlTemplate();
        payload.setName("new");
        payload.setSqlText("SELECT * FROM demo WHERE id = ?");
        payload.setWeight(3);
        payload.setDescription("prepared");
        payload.setExecutionMode("PREPARED_STATEMENT");
        payload.setParamJson("[{\"type\":\"INTEGER\",\"value\":1}]");
        when(repository.findById(4L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        SqlTemplate saved = controller.update(4L, payload);

        assertSame(existing, saved);
        assertEquals("new", existing.getName());
        assertEquals("SELECT * FROM demo WHERE id = ?", existing.getSqlText());
        assertEquals(3, existing.getWeight());
        assertEquals("prepared", existing.getDescription());
        assertEquals("PREPARED_STATEMENT", existing.getExecutionMode());
        assertEquals("[{\"type\":\"INTEGER\",\"value\":1}]", existing.getParamJson());
    }

    // Covers TemplateController#delete reference guard.
    @Test
    void shouldRejectDeleteWhileSqlLibIsReferenced() {
        SqlTemplateRepository repository = mock(SqlTemplateRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        SqlLibImportService importService = mock(SqlLibImportService.class);
        TemplateController controller = new TemplateController(repository, itemRepository, importService);
        when(itemRepository.countBySqlLibId(9L)).thenReturn(2L);

        IllegalArgumentException error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> controller.delete(9L)
        );

        assertEquals("SQL Lib 条目仍被 2 个测试集引用，无法删除", error.getMessage());
        verify(repository, never()).deleteById(9L);
    }
}
