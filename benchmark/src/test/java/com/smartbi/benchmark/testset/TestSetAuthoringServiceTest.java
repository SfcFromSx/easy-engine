package com.smartbi.benchmark.testset;

import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import com.smartbi.benchmark.domain.SqlTemplate;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import com.smartbi.benchmark.web.dto.TestSetItemListVo;
import com.smartbi.benchmark.web.dto.TestSetItemReorderRequest;
import com.smartbi.benchmark.web.dto.TestSetItemReferenceRequest;
import com.smartbi.benchmark.web.dto.TestSetTemplateCopyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestSetAuthoringServiceTest {

    // Covers create/update/delete item validation and ownership checks for SQL Lib references.
    @Test
    void shouldCreateUpdateAndDeleteOwnedItems() {
        BenchmarkTestSetRepository testSetRepository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        SqlTemplateRepository templateRepository = mock(SqlTemplateRepository.class);
        TestSetAuthoringService service = new TestSetAuthoringService(testSetRepository, itemRepository, templateRepository);

        BenchmarkTestSet set = new BenchmarkTestSet();
        ReflectionTestUtils.setField(set, "id", 5L);
        when(testSetRepository.findById(5L)).thenReturn(Optional.of(set));

        List<BenchmarkTestSetItem> store = new ArrayList<BenchmarkTestSetItem>();
        when(itemRepository.findByTestSetIdOrderBySortOrderAsc(5L)).thenAnswer(invocation -> sortCopy(store, 5L));
        when(itemRepository.save(any(BenchmarkTestSetItem.class))).thenAnswer(invocation -> {
            BenchmarkTestSetItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                ReflectionTestUtils.setField(item, "id", Long.valueOf(store.size() + 1L));
                store.add(item);
            }
            return item;
        });
        when(itemRepository.findById(1L)).thenAnswer(invocation -> findItem(store, 1L));
        doAnswer(invocation -> {
            BenchmarkTestSetItem item = invocation.getArgument(0);
            store.remove(item);
            return null;
        }).when(itemRepository).delete(any(BenchmarkTestSetItem.class));

        SqlTemplate first = new SqlTemplate();
        first.setId(2L);
        first.setName("First");
        first.setSqlText("SELECT 1");
        first.setWeight(2);
        first.setExecutionMode("STATEMENT");

        SqlTemplate second = new SqlTemplate();
        second.setId(3L);
        second.setName("Second");
        second.setSqlText("SELECT * FROM SALES WHERE ID = ?");
        second.setWeight(3);
        second.setExecutionMode("PREPARED_STATEMENT");
        second.setParamJson("[{\"type\":\"INTEGER\",\"value\":7}]");

        when(templateRepository.findById(2L)).thenReturn(Optional.of(first));
        when(templateRepository.findById(3L)).thenReturn(Optional.of(second));

        TestSetItemReferenceRequest createRequest = new TestSetItemReferenceRequest();
        createRequest.setSqlLibId(2L);

        BenchmarkTestSetItem created = service.createItem(5L, createRequest);
        assertEquals(Long.valueOf(1L), created.getId());
        assertEquals(Long.valueOf(2L), created.getSqlLibId());
        assertEquals("First", created.getLabel());
        assertEquals("SELECT 1", created.getSqlText());
        assertEquals(0, created.getSortOrder());
        assertEquals("STATEMENT", created.getExecutionMode());
        assertEquals(2, created.getWeight());

        TestSetItemReferenceRequest updateRequest = new TestSetItemReferenceRequest();
        updateRequest.setSqlLibId(3L);

        BenchmarkTestSetItem updated = service.updateItem(5L, 1L, updateRequest);
        assertEquals("Second", updated.getLabel());
        assertEquals("PREPARED_STATEMENT", updated.getExecutionMode());
        assertEquals("[{\"type\":\"INTEGER\",\"value\":7}]", updated.getParamJson());

        service.deleteItem(5L, 1L);
        assertEquals(0, store.size());
    }

    // Covers SQL Lib append order, duplicate references, list paging, and reorder normalization.
    @Test
    void shouldAddSqlLibItemsAndPersistRequestedOrder() {
        BenchmarkTestSetRepository testSetRepository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        SqlTemplateRepository templateRepository = mock(SqlTemplateRepository.class);
        TestSetAuthoringService service = new TestSetAuthoringService(testSetRepository, itemRepository, templateRepository);

        BenchmarkTestSet set = new BenchmarkTestSet();
        ReflectionTestUtils.setField(set, "id", 5L);
        when(testSetRepository.findById(5L)).thenReturn(Optional.of(set));

        List<BenchmarkTestSetItem> store = new ArrayList<BenchmarkTestSetItem>();
        BenchmarkTestSetItem existing = new BenchmarkTestSetItem();
        ReflectionTestUtils.setField(existing, "id", 10L);
        existing.setTestSetId(5L);
        existing.setSortOrder(0);
        existing.setSqlText("SELECT 0");
        existing.setWeight(1);
        existing.setExecutionMode("STATEMENT");
        store.add(existing);

        when(itemRepository.findByTestSetIdOrderBySortOrderAsc(5L)).thenAnswer(invocation -> sortCopy(store, 5L));
        when(itemRepository.save(any(BenchmarkTestSetItem.class))).thenAnswer(invocation -> {
            BenchmarkTestSetItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                ReflectionTestUtils.setField(item, "id", Long.valueOf(store.size() + 10L));
                store.add(item);
            }
            return item;
        });
        when(itemRepository.saveAll(any(Iterable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SqlTemplate first = new SqlTemplate();
        first.setId(2L);
        first.setName("First");
        first.setSqlText("SELECT 1");
        first.setWeight(2);
        first.setExecutionMode("STATEMENT");

        SqlTemplate second = new SqlTemplate();
        second.setId(3L);
        second.setName("Second");
        second.setSqlText("SELECT * FROM T WHERE ID = ?");
        second.setWeight(4);
        second.setExecutionMode("PREPARED_STATEMENT");
        second.setParamJson("[{\"type\":\"INTEGER\",\"value\":2}]");

        when(templateRepository.findAllById(any(Iterable.class))).thenReturn(Arrays.asList(first, second));
        existing.setSqlLibId(99L);
        when(templateRepository.findById(99L)).thenReturn(Optional.of(first));

        TestSetTemplateCopyRequest copyRequest = new TestSetTemplateCopyRequest();
        copyRequest.setSqlLibIds(Arrays.asList(3L, 2L, 3L));
        List<BenchmarkTestSetItem> copied = service.addSqlLibItems(5L, copyRequest);

        assertEquals(3, copied.size());
        assertEquals(Long.valueOf(3L), copied.get(0).getSqlLibId());
        assertEquals("Second", copied.get(0).getLabel());
        assertEquals(1, copied.get(0).getSortOrder());
        assertEquals(Long.valueOf(2L), copied.get(1).getSqlLibId());
        assertEquals("First", copied.get(1).getLabel());
        assertEquals(2, copied.get(1).getSortOrder());
        assertEquals("Second", copied.get(2).getLabel());
        assertEquals(3, copied.get(2).getSortOrder());

        Page<TestSetItemListVo> page = service.listItems(5L, 0, 2, "second");
        assertEquals(2L, page.getTotalElements());
        assertEquals("Second", page.getContent().get(0).getName());

        TestSetItemReorderRequest reorderRequest = new TestSetItemReorderRequest();
        reorderRequest.setItemIds(Arrays.asList(13L, 10L, 11L, 12L));
        List<BenchmarkTestSetItem> reordered = service.reorderItems(5L, reorderRequest);

        assertEquals(Long.valueOf(13L), reordered.get(0).getId());
        assertEquals(0, reordered.get(0).getSortOrder());
        assertEquals(Long.valueOf(10L), reordered.get(1).getId());
        assertEquals(1, reordered.get(1).getSortOrder());
    }

    // Covers validation failures for invalid payloads and wrong item ownership.
    @Test
    void shouldRejectInvalidPayloadsAndForeignItems() {
        BenchmarkTestSetRepository testSetRepository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        SqlTemplateRepository templateRepository = mock(SqlTemplateRepository.class);
        TestSetAuthoringService service = new TestSetAuthoringService(testSetRepository, itemRepository, templateRepository);

        BenchmarkTestSet set = new BenchmarkTestSet();
        ReflectionTestUtils.setField(set, "id", 5L);
        when(testSetRepository.findById(5L)).thenReturn(Optional.of(set));

        TestSetItemReferenceRequest blankSql = new TestSetItemReferenceRequest();
        assertThrows(IllegalArgumentException.class, () -> service.createItem(5L, blankSql));

        TestSetItemReferenceRequest missingSqlLib = new TestSetItemReferenceRequest();
        missingSqlLib.setSqlLibId(99L);
        assertThrows(IllegalArgumentException.class, () -> service.createItem(5L, missingSqlLib));

        BenchmarkTestSetItem foreign = new BenchmarkTestSetItem();
        ReflectionTestUtils.setField(foreign, "id", 9L);
        foreign.setTestSetId(8L);
        when(itemRepository.findById(9L)).thenReturn(Optional.of(foreign));
        TestSetItemReferenceRequest valid = new TestSetItemReferenceRequest();
        valid.setSqlLibId(1L);
        assertThrows(IllegalArgumentException.class, () -> service.updateItem(5L, 9L, valid));

        TestSetTemplateCopyRequest emptyCopy = new TestSetTemplateCopyRequest();
        emptyCopy.setSqlLibIds(Collections.<Long>emptyList());
        assertThrows(IllegalArgumentException.class, () -> service.addSqlLibItems(5L, emptyCopy));

        when(itemRepository.findByTestSetIdOrderBySortOrderAsc(5L)).thenReturn(Collections.<BenchmarkTestSetItem>emptyList());
        TestSetItemReorderRequest badReorder = new TestSetItemReorderRequest();
        badReorder.setItemIds(Collections.singletonList(1L));
        assertThrows(IllegalArgumentException.class, () -> service.reorderItems(5L, badReorder));
    }

    private static Optional<BenchmarkTestSetItem> findItem(List<BenchmarkTestSetItem> store, long id) {
        for (BenchmarkTestSetItem item : store) {
            if (item.getId() != null && item.getId().longValue() == id) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private static List<BenchmarkTestSetItem> sortCopy(List<BenchmarkTestSetItem> store, long testSetId) {
        List<BenchmarkTestSetItem> items = new ArrayList<BenchmarkTestSetItem>();
        for (BenchmarkTestSetItem item : store) {
            if (item.getTestSetId() != null && item.getTestSetId().longValue() == testSetId) {
                items.add(item);
            }
        }
        Collections.sort(items, (left, right) -> Integer.compare(left.getSortOrder(), right.getSortOrder()));
        return items;
    }
}
