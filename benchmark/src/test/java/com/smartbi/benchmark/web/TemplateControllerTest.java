package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.SqlTemplate;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateControllerTest {

    // Covers TemplateController#list keyword search branch.
    @Test
    void shouldUseSearchQueryWhenKeywordIsProvided() {
        SqlTemplateRepository repository = mock(SqlTemplateRepository.class);
        TemplateController controller = new TemplateController(repository);
        Page<SqlTemplate> expectedPage = new PageImpl<SqlTemplate>(Collections.singletonList(new SqlTemplate()));
        when(repository.searchByKeyword(eq("prepared"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(expectedPage);

        Page<SqlTemplate> page = controller.list(0, 10, "prepared", null);

        assertSame(expectedPage, page);
        verify(repository).searchByKeyword(eq("prepared"), any(org.springframework.data.domain.Pageable.class));
    }

    // Covers TemplateController#list execution-mode-only and default branches.
    @Test
    void shouldSupportExecutionModeFilteringAndListAllTemplatesWhenBlank() {
        SqlTemplateRepository repository = mock(SqlTemplateRepository.class);
        TemplateController controller = new TemplateController(repository);
        Page<SqlTemplate> expectedPage = new PageImpl<SqlTemplate>(Collections.singletonList(new SqlTemplate()));
        when(repository.findByExecutionModeOrderByIdAsc(eq("PREPARED_STATEMENT"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(expectedPage);
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(expectedPage);

        assertSame(expectedPage, controller.list(0, 10, null, "PREPARED_STATEMENT"));
        verify(repository).findByExecutionModeOrderByIdAsc(eq("PREPARED_STATEMENT"), any(org.springframework.data.domain.Pageable.class));

        Page<SqlTemplate> page = controller.list(0, 10, null, null);

        assertSame(expectedPage, page);
        verify(repository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    // Covers TemplateController#list combined keyword and execution-mode branch.
    @Test
    void shouldUseCombinedSearchWhenKeywordAndExecutionModeAreProvided() {
        SqlTemplateRepository repository = mock(SqlTemplateRepository.class);
        TemplateController controller = new TemplateController(repository);
        Page<SqlTemplate> expectedPage = new PageImpl<SqlTemplate>(Collections.singletonList(new SqlTemplate()));
        when(repository.searchByKeywordAndExecutionMode(eq("prepared"), eq("PREPARED_STATEMENT"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(expectedPage);

        Page<SqlTemplate> page = controller.list(0, 10, " prepared ", " PREPARED_STATEMENT ");

        assertSame(expectedPage, page);
        verify(repository).searchByKeywordAndExecutionMode(
                eq("prepared"),
                eq("PREPARED_STATEMENT"),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    // Covers TemplateController#update editable field propagation.
    @Test
    void shouldUpdateTemplateFieldsIncludingExecutionModeAndParams() {
        SqlTemplateRepository repository = mock(SqlTemplateRepository.class);
        TemplateController controller = new TemplateController(repository);
        SqlTemplate existing = new SqlTemplate();
        existing.setId(4L);
        existing.setName("old");
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
}
