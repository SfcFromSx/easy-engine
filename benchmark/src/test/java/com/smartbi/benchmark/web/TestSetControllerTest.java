package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.testset.TestSetAuthoringService;
import com.smartbi.benchmark.testset.TestSetImportService;
import com.smartbi.benchmark.web.dto.TestSetItemReorderRequest;
import com.smartbi.benchmark.web.dto.TestSetListVo;
import com.smartbi.benchmark.web.dto.TestSetItemWriteRequest;
import com.smartbi.benchmark.web.dto.TestSetTemplateCopyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestSetControllerTest {

    // Covers TestSetController#list and TestSetController#items read-model shaping.
    @Test
    void shouldBuildListViewModelsAndExposeItems() {
        TestSetImportService importService = mock(TestSetImportService.class);
        TestSetAuthoringService authoringService = mock(TestSetAuthoringService.class);
        BenchmarkTestSetRepository repository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        TestSetController controller = new TestSetController(importService, authoringService, repository, itemRepository);

        BenchmarkTestSet testSet = new BenchmarkTestSet();
        ReflectionTestUtils.setField(testSet, "id", 1L);
        testSet.setName("Orders");
        testSet.setDescription("desc");
        testSet.setSourceFilename("orders.xlsx");
        ReflectionTestUtils.setField(testSet, "createdAt", Instant.parse("2026-03-31T00:00:00Z"));
        BenchmarkTestSetItem item = new BenchmarkTestSetItem();
        ReflectionTestUtils.setField(item, "id", 3L);
        item.setTestSetId(1L);
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.singletonList(testSet));
        when(itemRepository.countByTestSetId(1L)).thenReturn(2L);
        when(itemRepository.findByTestSetIdOrderBySortOrderAsc(1L)).thenReturn(Collections.singletonList(item));

        List<TestSetListVo> view = controller.list();

        assertEquals(1, view.size());
        assertEquals("Orders", view.get(0).getName());
        assertEquals(2L, view.get(0).getItemCount());
        assertEquals(Collections.singletonList(item), controller.items(1L));
    }

    // Covers TestSetController#create and TestSetController#update validation and trimming.
    @Test
    void shouldTrimEditableFieldsAndRejectBlankNames() {
        TestSetImportService importService = mock(TestSetImportService.class);
        TestSetAuthoringService authoringService = mock(TestSetAuthoringService.class);
        BenchmarkTestSetRepository repository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        TestSetController controller = new TestSetController(importService, authoringService, repository, itemRepository);

        BenchmarkTestSet payload = new BenchmarkTestSet();
        payload.setName("  Orders  ");
        payload.setDescription("  Imported  ");
        when(repository.save(any(BenchmarkTestSet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BenchmarkTestSet created = controller.create(payload);

        assertEquals("Orders", created.getName());
        assertEquals("Imported", created.getDescription());

        BenchmarkTestSet existing = new BenchmarkTestSet();
        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        BenchmarkTestSet updated = controller.update(2L, payload);
        assertEquals("Orders", updated.getName());

        BenchmarkTestSet invalid = new BenchmarkTestSet();
        invalid.setName("   ");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> controller.create(invalid));
        assertEquals("测试集名称不能为空", error.getMessage());
    }

    // Covers TestSetController#upload checked-exception wrapping and illegal-argument passthrough.
    @Test
    void shouldTranslateImportFailuresDuringUpload() throws Exception {
        TestSetImportService importService = mock(TestSetImportService.class);
        TestSetAuthoringService authoringService = mock(TestSetAuthoringService.class);
        BenchmarkTestSetRepository repository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        TestSetController controller = new TestSetController(importService, authoringService, repository, itemRepository);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cases.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3}
        );

        when(importService.importFromExcel(eq(file), eq("named-set"), eq("desc"))).thenThrow(new Exception("sheet broken"));
        IllegalArgumentException wrapped = assertThrows(IllegalArgumentException.class, () -> controller.upload(file, "named-set", "desc"));
        assertEquals("Excel 解析失败: sheet broken", wrapped.getMessage());

        when(importService.importFromExcel(eq(file), eq(null), eq(null))).thenThrow(new IllegalArgumentException("文件为空"));
        IllegalArgumentException original = assertThrows(IllegalArgumentException.class, () -> controller.upload(file, null, null));
        assertEquals("文件为空", original.getMessage());
    }

    // Covers TestSetController#upload success payload shaping with optional metadata.
    @Test
    void shouldReturnUploadedTestSetMetadata() throws Exception {
        TestSetImportService importService = mock(TestSetImportService.class);
        TestSetAuthoringService authoringService = mock(TestSetAuthoringService.class);
        BenchmarkTestSetRepository repository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        TestSetController controller = new TestSetController(importService, authoringService, repository, itemRepository);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cases.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1}
        );
        BenchmarkTestSet imported = new BenchmarkTestSet();
        ReflectionTestUtils.setField(imported, "id", 6L);
        imported.setName("Imported");
        when(importService.importFromExcel(eq(file), eq("Named"), eq("Desc"))).thenReturn(imported);
        when(itemRepository.countByTestSetId(6L)).thenReturn(4L);

        Map<String, Object> payload = controller.upload(file, "Named", "Desc");

        assertEquals(imported, payload.get("testSet"));
        assertEquals(4L, payload.get("itemCount"));
    }

    // Covers TestSetController item-authoring endpoints delegating to the authoring service.
    @Test
    void shouldDelegateItemAuthoringEndpoints() {
        TestSetImportService importService = mock(TestSetImportService.class);
        TestSetAuthoringService authoringService = mock(TestSetAuthoringService.class);
        BenchmarkTestSetRepository repository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        TestSetController controller = new TestSetController(importService, authoringService, repository, itemRepository);

        BenchmarkTestSetItem item = new BenchmarkTestSetItem();
        ReflectionTestUtils.setField(item, "id", 9L);
        item.setTestSetId(3L);
        TestSetItemWriteRequest writeRequest = new TestSetItemWriteRequest();
        TestSetTemplateCopyRequest copyRequest = new TestSetTemplateCopyRequest();
        copyRequest.setTemplateIds(Collections.singletonList(7L));
        TestSetItemReorderRequest reorderRequest = new TestSetItemReorderRequest();
        reorderRequest.setItemIds(Collections.singletonList(9L));

        when(authoringService.createItem(3L, writeRequest)).thenReturn(item);
        when(authoringService.updateItem(3L, 9L, writeRequest)).thenReturn(item);
        when(authoringService.copyTemplates(3L, copyRequest)).thenReturn(Collections.singletonList(item));
        when(authoringService.reorderItems(3L, reorderRequest)).thenReturn(Collections.singletonList(item));

        assertEquals(item, controller.createItem(3L, writeRequest));
        assertEquals(item, controller.updateItem(3L, 9L, writeRequest));
        assertEquals(Collections.singletonList(item), controller.copyTemplates(3L, copyRequest));
        assertEquals(Collections.singletonList(item), controller.reorderItems(3L, reorderRequest));
        controller.deleteItem(3L, 9L);

        verify(authoringService).deleteItem(3L, 9L);
    }
}
