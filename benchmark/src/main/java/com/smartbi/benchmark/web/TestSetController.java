package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.testset.TestSetAuthoringService;
import com.smartbi.benchmark.web.dto.TestSetItemReorderRequest;
import com.smartbi.benchmark.web.dto.TestSetItemListVo;
import com.smartbi.benchmark.web.dto.TestSetListVo;
import com.smartbi.benchmark.web.dto.TestSetItemReferenceRequest;
import com.smartbi.benchmark.web.dto.TestSetTemplateCopyRequest;
import org.springframework.data.domain.Page;
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

    private final TestSetAuthoringService authoringService;
    private final BenchmarkTestSetRepository testSetRepository;
    private final BenchmarkTestSetItemRepository itemRepository;

    public TestSetController(TestSetAuthoringService authoringService,
                             BenchmarkTestSetRepository testSetRepository,
                             BenchmarkTestSetItemRepository itemRepository) {
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
    public Page<TestSetItemListVo> items(@PathVariable long id,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) String keyword) {
        return authoringService.listItems(id, page, size, keyword);
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
        throw new IllegalStateException("测试集不再支持直接上传，请先上传到 SQL Lib 再选择 SQL。");
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        testSetRepository.deleteById(id);
    }

    @PostMapping("/{id}/items")
    public Object createItem(@PathVariable long id,
                             @RequestBody TestSetItemReferenceRequest request) {
        return authoringService.createItem(id, request);
    }

    @PutMapping("/{id}/items/{itemId}")
    public Object updateItem(@PathVariable long id,
                             @PathVariable long itemId,
                             @RequestBody TestSetItemReferenceRequest request) {
        return authoringService.updateItem(id, itemId, request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public void deleteItem(@PathVariable long id, @PathVariable long itemId) {
        authoringService.deleteItem(id, itemId);
    }

    @PostMapping("/{id}/items/add-sql-lib")
    public List<?> addSqlLibItems(@PathVariable long id,
                                  @RequestBody TestSetTemplateCopyRequest request) {
        return authoringService.addSqlLibItems(id, request);
    }

    @PostMapping("/{id}/items/copy-templates")
    public List<?> copyTemplates(@PathVariable long id,
                                 @RequestBody TestSetTemplateCopyRequest request) {
        return authoringService.addSqlLibItems(id, request);
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
