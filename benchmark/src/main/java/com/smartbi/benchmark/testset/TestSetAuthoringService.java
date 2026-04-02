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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    @Transactional(readOnly = true)
    public Page<TestSetItemListVo> listItems(long testSetId, int page, int size, String keyword) {
        requireTestSet(testSetId);
        List<BenchmarkTestSetItem> items = itemRepository.findByTestSetIdOrderBySortOrderAsc(testSetId);
        List<TestSetItemListVo> view = new ArrayList<TestSetItemListVo>();
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        for (BenchmarkTestSetItem item : items) {
            SqlTemplate sqlLib = resolveSqlLib(item);
            TestSetItemListVo row = toView(item, sqlLib);
            if (matchesKeyword(row, normalizedKeyword)) {
                view.add(row);
            }
        }

        int safeSize = size < 1 ? 20 : size;
        int safePage = page < 0 ? 0 : page;
        int fromIndex = Math.min(safePage * safeSize, view.size());
        int toIndex = Math.min(fromIndex + safeSize, view.size());
        return new PageImpl<TestSetItemListVo>(
                view.subList(fromIndex, toIndex),
                PageRequest.of(safePage, safeSize),
                view.size());
    }

    @Transactional
    public BenchmarkTestSetItem createItem(long testSetId, TestSetItemReferenceRequest request) {
        requireTestSet(testSetId);
        SqlTemplate sqlLib = requireSqlLib(resolveSqlLibId(request));
        BenchmarkTestSetItem item = new BenchmarkTestSetItem();
        item.setTestSetId(testSetId);
        item.setSortOrder(itemRepository.findByTestSetIdOrderBySortOrderAsc(testSetId).size());
        applySqlLibReference(item, sqlLib);
        return itemRepository.save(item);
    }

    @Transactional
    public BenchmarkTestSetItem updateItem(long testSetId, long itemId, TestSetItemReferenceRequest request) {
        BenchmarkTestSetItem item = requireOwnedItem(testSetId, itemId);
        SqlTemplate sqlLib = requireSqlLib(resolveSqlLibId(request));
        applySqlLibReference(item, sqlLib);
        return itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(long testSetId, long itemId) {
        BenchmarkTestSetItem item = requireOwnedItem(testSetId, itemId);
        itemRepository.delete(item);
        normalizeSortOrder(testSetId);
    }

    @Transactional
    public List<BenchmarkTestSetItem> addSqlLibItems(long testSetId, TestSetTemplateCopyRequest request) {
        requireTestSet(testSetId);
        List<Long> sqlLibIds = request == null ? null : request.resolveSqlLibIds();
        if (sqlLibIds == null || sqlLibIds.isEmpty()) {
            throw new IllegalArgumentException("请选择至少一个 SQL Lib 条目");
        }

        Map<Long, SqlTemplate> templatesById = new HashMap<Long, SqlTemplate>();
        List<SqlTemplate> existingTemplates = templateRepository.findAllById(new HashSet<Long>(sqlLibIds));
        for (SqlTemplate template : existingTemplates) {
            templatesById.put(template.getId(), template);
        }

        int nextOrder = itemRepository.findByTestSetIdOrderBySortOrderAsc(testSetId).size();
        List<BenchmarkTestSetItem> created = new ArrayList<BenchmarkTestSetItem>();
        for (Long templateId : sqlLibIds) {
            SqlTemplate template = templatesById.get(templateId);
            if (template == null) {
                throw new IllegalArgumentException("SQL Lib 条目不存在: " + templateId);
            }
            BenchmarkTestSetItem item = new BenchmarkTestSetItem();
            item.setTestSetId(testSetId);
            item.setSortOrder(nextOrder++);
            applySqlLibReference(item, template);
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

    private void applySqlLibReference(BenchmarkTestSetItem target, SqlTemplate sqlLib) {
        target.setSqlLibId(sqlLib.getId());
        target.setLabel(StringUtils.hasText(sqlLib.getName()) ? sqlLib.getName().trim() : null);
        target.setSqlText(sqlLib.getSqlText());
        target.setWeight(sqlLib.getWeight() < 1 ? 1 : sqlLib.getWeight());
        target.setExecutionMode(normalizeExecutionMode(sqlLib.getExecutionMode()));
        target.setParamJson(sanitizeParamJson(target.getExecutionMode(), sqlLib.getParamJson()));
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

    private SqlTemplate requireSqlLib(Long sqlLibId) {
        if (sqlLibId == null) {
            throw new IllegalArgumentException("请选择 SQL Lib 条目");
        }
        return templateRepository.findById(sqlLibId)
                .orElseThrow(() -> new IllegalArgumentException("SQL Lib 条目不存在: " + sqlLibId));
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

    private static Long resolveSqlLibId(TestSetItemReferenceRequest request) {
        return request == null ? null : request.getSqlLibId();
    }

    private SqlTemplate resolveSqlLib(BenchmarkTestSetItem item) {
        if (item.getSqlLib() != null) {
            return item.getSqlLib();
        }
        if (item.getSqlLibId() == null) {
            return null;
        }
        return templateRepository.findById(item.getSqlLibId()).orElse(null);
    }

    private static TestSetItemListVo toView(BenchmarkTestSetItem item, SqlTemplate sqlLib) {
        String name = sqlLib != null && StringUtils.hasText(sqlLib.getName()) ? sqlLib.getName() : item.getLabel();
        String sqlText = sqlLib != null && StringUtils.hasText(sqlLib.getSqlText()) ? sqlLib.getSqlText() : item.getSqlText();
        int weight = sqlLib != null ? Math.max(1, sqlLib.getWeight()) : Math.max(1, item.getWeight());
        String executionMode = sqlLib != null ? sqlLib.getExecutionMode() : item.getExecutionMode();
        String paramJson = sqlLib != null ? sqlLib.getParamJson() : item.getParamJson();
        String sourceFilename = sqlLib != null ? sqlLib.getSourceFilename() : null;
        return new TestSetItemListVo(
                item.getId(),
                item.getSortOrder(),
                item.getSqlLibId(),
                name,
                sqlText,
                weight,
                executionMode,
                paramJson,
                sourceFilename,
                sqlLib != null ? sqlLib.getUploadedAt() : null
        );
    }

    private static boolean matchesKeyword(TestSetItemListVo row, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return contains(row.getName(), keyword)
                || contains(row.getSqlText(), keyword)
                || contains(row.getExecutionMode(), keyword)
                || contains(row.getParamJson(), keyword)
                || contains(row.getSourceFilename(), keyword)
                || String.valueOf(row.getSqlLibId()).contains(keyword)
                || String.valueOf(row.getSortOrder()).contains(keyword);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
