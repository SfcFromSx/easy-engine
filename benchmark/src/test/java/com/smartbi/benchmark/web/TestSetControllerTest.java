package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.testset.TestSetAuthoringService;
import com.smartbi.benchmark.web.dto.TestSetItemListVo;
import com.smartbi.benchmark.web.dto.TestSetItemReorderRequest;
import com.smartbi.benchmark.web.dto.TestSetListVo;
import com.smartbi.benchmark.web.dto.TestSetItemReferenceRequest;
import com.smartbi.benchmark.web.dto.TestSetTemplateCopyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestSetControllerTest {

    // Covers TestSetController#list and TestSetController#items read-model shaping.
    @Test
    void shouldBuildListViewModelsAndExposeItems() {
        TestSetAuthoringService authoringService = mock(TestSetAuthoringService.class);
        BenchmarkTestSetRepository repository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        TestSetController controller = new TestSetController(authoringService, repository, itemRepository);

        BenchmarkTestSet testSet = new BenchmarkTestSet();
        ReflectionTestUtils.setField(testSet, "id", 1L);
        testSet.setName("Orders");
        testSet.setDescription("desc");
        testSet.setSourceFilename("orders.xlsx");
        ReflectionTestUtils.setField(testSet, "createdAt", Instant.parse("2026-03-31T00:00:00Z"));
        TestSetItemListVo item = new TestSetItemListVo(3L, 0, 8L, "SQL A", "SELECT 1", 1, "STATEMENT", null, "orders.xlsx", Instant.now());
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.singletonList(testSet));
        when(itemRepository.countByTestSetId(1L)).thenReturn(2L);
        when(authoringService.listItems(1L, 0, 20, null))
                .thenReturn(new PageImpl<TestSetItemListVo>(Collections.singletonList(item)));

        List<TestSetListVo> view = controller.list();

        assertEquals(1, view.size());
        assertEquals("Orders", view.get(0).getName());
        assertEquals(2L, view.get(0).getItemCount());
        Page<TestSetItemListVo> page = controller.items(1L, 0, 20, null);
        assertEquals(1, page.getContent().size());
        assertEquals("SQL A", page.getContent().get(0).getName());
    }

    // Covers TestSetController#create and TestSetController#update validation and trimming.
    @Test
    void shouldTrimEditableFieldsAndRejectBlankNames() {
        TestSetAuthoringService authoringService = mock(TestSetAuthoringService.class);
        BenchmarkTestSetRepository repository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        TestSetController controller = new TestSetController(authoringService, repository, itemRepository);

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

    // Covers TestSetController#upload rejecting legacy direct test-set imports.
    @Test
    void shouldRejectLegacyDirectTestSetUpload() {
        TestSetAuthoringService authoringService = mock(TestSetAuthoringService.class);
        BenchmarkTestSetRepository repository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        TestSetController controller = new TestSetController(authoringService, repository, itemRepository);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> controller.upload(null, "Named", "Desc"));
        assertEquals("测试集不再支持直接上传，请先上传到 SQL Lib 再选择 SQL。", error.getMessage());
    }

    // Covers TestSetController item-authoring endpoints delegating to the authoring service.
    @Test
    void shouldDelegateItemAuthoringEndpoints() {
        TestSetAuthoringService authoringService = mock(TestSetAuthoringService.class);
        BenchmarkTestSetRepository repository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        TestSetController controller = new TestSetController(authoringService, repository, itemRepository);

        BenchmarkTestSetItem item = new BenchmarkTestSetItem();
        ReflectionTestUtils.setField(item, "id", 9L);
        item.setTestSetId(3L);
        TestSetItemReferenceRequest writeRequest = new TestSetItemReferenceRequest();
        writeRequest.setSqlLibId(7L);
        TestSetTemplateCopyRequest copyRequest = new TestSetTemplateCopyRequest();
        copyRequest.setSqlLibIds(Collections.singletonList(7L));
        TestSetItemReorderRequest reorderRequest = new TestSetItemReorderRequest();
        reorderRequest.setItemIds(Collections.singletonList(9L));

        when(authoringService.createItem(3L, writeRequest)).thenReturn(item);
        when(authoringService.updateItem(3L, 9L, writeRequest)).thenReturn(item);
        when(authoringService.addSqlLibItems(3L, copyRequest)).thenReturn(Collections.singletonList(item));
        when(authoringService.reorderItems(3L, reorderRequest)).thenReturn(Collections.singletonList(item));

        assertEquals(item, controller.createItem(3L, writeRequest));
        assertEquals(item, controller.updateItem(3L, 9L, writeRequest));
        assertEquals(Collections.singletonList(item), controller.addSqlLibItems(3L, copyRequest));
        assertEquals(Collections.singletonList(item), controller.copyTemplates(3L, copyRequest));
        assertEquals(Collections.singletonList(item), controller.reorderItems(3L, reorderRequest));
        controller.deleteItem(3L, 9L);

        verify(authoringService).deleteItem(3L, 9L);
    }
}
