package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.testset.TestSetImportService;
import com.smartbi.benchmark.web.dto.TestSetListVo;
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
    private final BenchmarkTestSetRepository testSetRepository;
    private final BenchmarkTestSetItemRepository itemRepository;

    public TestSetController(TestSetImportService importService,
                             BenchmarkTestSetRepository testSetRepository,
                             BenchmarkTestSetItemRepository itemRepository) {
        this.importService = importService;
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
            @RequestParam(value = "name", required = false) String name) {
        try {
            BenchmarkTestSet set = importService.importFromExcel(file, name);
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

    private static void applyEditableFields(BenchmarkTestSet target, BenchmarkTestSet source) {
        if (source == null || !StringUtils.hasText(source.getName())) {
            throw new IllegalArgumentException("测试集名称不能为空");
        }
        target.setName(source.getName().trim());
        target.setDescription(StringUtils.hasText(source.getDescription()) ? source.getDescription().trim() : null);
    }
}
