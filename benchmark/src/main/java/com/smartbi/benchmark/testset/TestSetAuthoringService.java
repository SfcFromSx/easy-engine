package com.smartbi.benchmark.testset;

import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import com.smartbi.benchmark.domain.SqlTemplate;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import com.smartbi.benchmark.web.dto.TestSetItemReorderRequest;
import com.smartbi.benchmark.web.dto.TestSetItemWriteRequest;
import com.smartbi.benchmark.web.dto.TestSetTemplateCopyRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TestSetAuthoringService {

    private static final String MODE_STATEMENT = "STATEMENT";
    private static final String MODE_PREPARED = "PREPARED_STATEMENT";

    private final BenchmarkTestSetRepository testSetRepository;
    private final BenchmarkTestSetItemRepository itemRepository;
    private final SqlTemplateRepository templateRepository;

    public TestSetAuthoringService(BenchmarkTestSetRepository testSetRepository,
                                   BenchmarkTestSetItemRepository itemRepository,
                                   SqlTemplateRepository templateRepository) {
        this.testSetRepository = testSetRepository;
        this.itemRepository = itemRepository;
        this.templateRepository = templateRepository;
    }

    @Transactional
    public BenchmarkTestSetItem createItem(long testSetId, TestSetItemWriteRequest request) {
        requireTestSet(testSetId);
        BenchmarkTestSetItem item = new BenchmarkTestSetItem();
        item.setTestSetId(testSetId);
        item.setSortOrder(itemRepository.findByTestSetIdOrderBySortOrderAsc(testSetId).size());
        applyRequest(item, request);
        return itemRepository.save(item);
    }

    @Transactional
    public BenchmarkTestSetItem updateItem(long testSetId, long itemId, TestSetItemWriteRequest request) {
        BenchmarkTestSetItem item = requireOwnedItem(testSetId, itemId);
        applyRequest(item, request);
        return itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(long testSetId, long itemId) {
        BenchmarkTestSetItem item = requireOwnedItem(testSetId, itemId);
        itemRepository.delete(item);
        normalizeSortOrder(testSetId);
    }

    @Transactional
    public List<BenchmarkTestSetItem> copyTemplates(long testSetId, TestSetTemplateCopyRequest request) {
        requireTestSet(testSetId);
        if (request == null || request.getTemplateIds() == null || request.getTemplateIds().isEmpty()) {
            throw new IllegalArgumentException("请选择至少一个 SQL 模板");
        }

        List<Long> templateIds = request.getTemplateIds();
        Map<Long, SqlTemplate> templatesById = new HashMap<Long, SqlTemplate>();
        List<SqlTemplate> existingTemplates = templateRepository.findAllById(new HashSet<Long>(templateIds));
        for (SqlTemplate template : existingTemplates) {
            templatesById.put(template.getId(), template);
        }

        int nextOrder = itemRepository.findByTestSetIdOrderBySortOrderAsc(testSetId).size();
        List<BenchmarkTestSetItem> created = new ArrayList<BenchmarkTestSetItem>();
        for (Long templateId : templateIds) {
            SqlTemplate template = templatesById.get(templateId);
            if (template == null) {
                throw new IllegalArgumentException("SQL 模板不存在: " + templateId);
            }
            BenchmarkTestSetItem item = new BenchmarkTestSetItem();
            item.setTestSetId(testSetId);
            item.setSortOrder(nextOrder++);
            item.setLabel(StringUtils.hasText(template.getName()) ? template.getName().trim() : null);
            item.setSqlText(template.getSqlText());
            item.setWeight(template.getWeight() < 1 ? 1 : template.getWeight());
            item.setExecutionMode(normalizeExecutionMode(template.getExecutionMode()));
            item.setParamJson(sanitizeParamJson(item.getExecutionMode(), template.getParamJson()));
            created.add(itemRepository.save(item));
        }
        return created;
    }

    @Transactional
    public List<BenchmarkTestSetItem> reorderItems(long testSetId, TestSetItemReorderRequest request) {
        requireTestSet(testSetId);
        List<BenchmarkTestSetItem> items = itemRepository.findByTestSetIdOrderBySortOrderAsc(testSetId);
        if (request == null || request.getItemIds() == null) {
            throw new IllegalArgumentException("请提供完整的测试集顺序");
        }

        List<Long> orderedIds = request.getItemIds();
        if (orderedIds.size() != items.size()) {
            throw new IllegalArgumentException("测试集顺序与当前 SQL 数量不一致");
        }

        Map<Long, BenchmarkTestSetItem> itemsById = new HashMap<Long, BenchmarkTestSetItem>();
        for (BenchmarkTestSetItem item : items) {
            itemsById.put(item.getId(), item);
        }

        Set<Long> seen = new HashSet<Long>();
        List<BenchmarkTestSetItem> reordered = new ArrayList<BenchmarkTestSetItem>();
        for (int i = 0; i < orderedIds.size(); i++) {
            Long itemId = orderedIds.get(i);
            BenchmarkTestSetItem item = itemsById.get(itemId);
            if (item == null || !seen.add(itemId)) {
                throw new IllegalArgumentException("测试集顺序包含无效 SQL 项: " + itemId);
            }
            item.setSortOrder(i);
            reordered.add(item);
        }
        itemRepository.saveAll(reordered);
        return itemRepository.findByTestSetIdOrderBySortOrderAsc(testSetId);
    }

    private void applyRequest(BenchmarkTestSetItem target, TestSetItemWriteRequest request) {
        if (request == null || !StringUtils.hasText(request.getSqlText())) {
            throw new IllegalArgumentException("SQL 内容不能为空");
        }
        int weight = request.getWeight() == null ? 1 : request.getWeight().intValue();
        if (weight < 1) {
            throw new IllegalArgumentException("权重必须大于或等于 1");
        }

        String mode = normalizeExecutionMode(request.getExecutionMode());
        target.setLabel(StringUtils.hasText(request.getLabel()) ? request.getLabel().trim() : null);
        target.setSqlText(request.getSqlText().trim());
        target.setWeight(weight);
        target.setExecutionMode(mode);
        target.setParamJson(sanitizeParamJson(mode, request.getParamJson()));
    }

    private BenchmarkTestSet requireTestSet(long testSetId) {
        return testSetRepository.findById(testSetId)
                .orElseThrow(() -> new IllegalArgumentException("测试集不存在: " + testSetId));
    }

    private BenchmarkTestSetItem requireOwnedItem(long testSetId, long itemId) {
        requireTestSet(testSetId);
        BenchmarkTestSetItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("测试集 SQL 不存在: " + itemId));
        if (item.getTestSetId() == null || item.getTestSetId().longValue() != testSetId) {
            throw new IllegalArgumentException("测试集 SQL 不属于当前测试集: " + itemId);
        }
        return item;
    }

    private void normalizeSortOrder(long testSetId) {
        List<BenchmarkTestSetItem> items = itemRepository.findByTestSetIdOrderBySortOrderAsc(testSetId);
        boolean changed = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getSortOrder() != i) {
                items.get(i).setSortOrder(i);
                changed = true;
            }
        }
        if (changed) {
            itemRepository.saveAll(items);
        }
    }

    private String normalizeExecutionMode(String executionMode) {
        if (!StringUtils.hasText(executionMode)) {
            return MODE_STATEMENT;
        }
        String normalized = executionMode.trim().toUpperCase(Locale.ROOT);
        if (!MODE_STATEMENT.equals(normalized) && !MODE_PREPARED.equals(normalized)) {
            throw new IllegalArgumentException("不支持的执行模式: " + executionMode);
        }
        return normalized;
    }

    private String sanitizeParamJson(String executionMode, String paramJson) {
        if (!MODE_PREPARED.equals(executionMode)) {
            return null;
        }
        return StringUtils.hasText(paramJson) ? paramJson.trim() : null;
    }
}
