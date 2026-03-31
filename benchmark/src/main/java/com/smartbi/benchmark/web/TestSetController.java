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
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test-sets")
public class TestSetController {

    private final TestSetImportService importService;
    private final TestSetAuthoringService authoringService;
    private final BenchmarkTestSetRepository testSetRepository;
    private final BenchmarkTestSetItemRepository itemRepository;

    public TestSetController(TestSetImportService importService,
                             TestSetAuthoringService authoringService,
                             BenchmarkTestSetRepository testSetRepository,
                             BenchmarkTestSetItemRepository itemRepository) {
        this.importService = importService;
        this.authoringService = authoringService;
        this.testSetRepository = testSetRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public List<TestSetListVo> list() {
        List<BenchmarkTestSet> sets = testSetRepository.findAllByOrderByCreatedAtDesc();
        List<TestSetListVo> out = new ArrayList<>();
        for (BenchmarkTestSet s : sets) {
            long cnt = itemRepository.countByTestSetId(s.getId());
            out.add(new TestSetListVo(
                    s.getId(),
                    s.getName(),
                    s.getDescription(),
                    s.getCreatedAt(),
                    s.getSourceFilename(),
                    cnt));
        }
        return out;
    }

    @GetMapping("/{id}/items")
    public List<BenchmarkTestSetItem> items(@PathVariable long id) {
        return itemRepository.findByTestSetIdOrderBySortOrderAsc(id);
    }

    @PostMapping
    public BenchmarkTestSet create(@RequestBody BenchmarkTestSet testSet) {
        BenchmarkTestSet entity = new BenchmarkTestSet();
        applyEditableFields(entity, testSet);
        return testSetRepository.save(entity);
    }

    @PutMapping("/{id}")
    public BenchmarkTestSet update(@PathVariable long id, @RequestBody BenchmarkTestSet testSet) {
        BenchmarkTestSet entity = testSetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("测试集不存在: " + id));
        applyEditableFields(entity, testSet);
        return testSetRepository.save(entity);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description) {
        try {
            BenchmarkTestSet set = importService.importFromExcel(file, name, description);
            long cnt = itemRepository.countByTestSetId(set.getId());
            Map<String, Object> m = new HashMap<>();
            m.put("testSet", set);
            m.put("itemCount", cnt);
            return m;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Excel 解析失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        testSetRepository.deleteById(id);
    }

    @PostMapping("/{id}/items")
    public BenchmarkTestSetItem createItem(@PathVariable long id,
                                           @RequestBody TestSetItemWriteRequest request) {
        return authoringService.createItem(id, request);
    }

    @PutMapping("/{id}/items/{itemId}")
    public BenchmarkTestSetItem updateItem(@PathVariable long id,
                                           @PathVariable long itemId,
                                           @RequestBody TestSetItemWriteRequest request) {
        return authoringService.updateItem(id, itemId, request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public void deleteItem(@PathVariable long id, @PathVariable long itemId) {
        authoringService.deleteItem(id, itemId);
    }

    @PostMapping("/{id}/items/copy-templates")
    public List<BenchmarkTestSetItem> copyTemplates(@PathVariable long id,
                                                    @RequestBody TestSetTemplateCopyRequest request) {
        return authoringService.copyTemplates(id, request);
    }

    @PutMapping("/{id}/items/reorder")
    public List<BenchmarkTestSetItem> reorderItems(@PathVariable long id,
                                                   @RequestBody TestSetItemReorderRequest request) {
        return authoringService.reorderItems(id, request);
    }

    private static void applyEditableFields(BenchmarkTestSet target, BenchmarkTestSet source) {
        if (source == null || !StringUtils.hasText(source.getName())) {
            throw new IllegalArgumentException("测试集名称不能为空");
        }
        target.setName(source.getName().trim());
        target.setDescription(StringUtils.hasText(source.getDescription()) ? source.getDescription().trim() : null);
    }
}
