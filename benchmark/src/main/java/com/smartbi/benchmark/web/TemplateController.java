package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.SqlTemplate;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import com.smartbi.benchmark.sql.SqlLibImportService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/sql-lib", "/api/v1/templates"})
public class TemplateController {

    private final SqlTemplateRepository repository;
    private final BenchmarkTestSetItemRepository itemRepository;
    private final SqlLibImportService importService;

    public TemplateController(SqlTemplateRepository repository,
                              BenchmarkTestSetItemRepository itemRepository,
                              SqlLibImportService importService) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.importService = importService;
    }

    @GetMapping
    public Page<SqlTemplate> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String executionMode,
            @RequestParam(required = false) String sourceFilename,
            @RequestParam(required = false) Instant uploadedAfter,
            @RequestParam(required = false) Instant uploadedBefore) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Order.desc("uploadedAt"), Sort.Order.desc("id")));
        return repository.searchLibrary(
                trimToNull(keyword),
                trimToNull(executionMode),
                trimToNull(sourceFilename),
                uploadedAfter,
                uploadedBefore,
                pageRequest);
    }

    @GetMapping("/{id}")
    public SqlTemplate get(@PathVariable long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("SQL Lib 条目不存在: " + id));
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestPart("file") MultipartFile file) throws Exception {
        List<SqlTemplate> templates = importService.importFromFile(file);
        String sourceFilename = file.getOriginalFilename();
        Instant uploadedAt = Instant.now();
        for (SqlTemplate template : templates) {
            template.setId(null);
            template.setSourceFilename(sourceFilename);
            template.setUploadedAt(uploadedAt);
            applyEditableFields(template, template, false);
        }
        repository.saveAll(templates);
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("count", templates.size());
        response.put("sourceFilename", sourceFilename);
        return response;
    }

    @PostMapping
    public SqlTemplate create(@RequestBody SqlTemplate t) {
        SqlTemplate entity = new SqlTemplate();
        applyEditableFields(entity, t, false);
        entity.setId(null);
        return repository.save(entity);
    }

    @PutMapping("/{id}")
    public SqlTemplate update(@PathVariable long id, @RequestBody SqlTemplate t) {
        SqlTemplate entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("SQL Lib 条目不存在: " + id));
        applyEditableFields(entity, t, true);
        return repository.save(entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        long references = itemRepository.countBySqlLibId(id);
        if (references > 0) {
            throw new IllegalArgumentException("SQL Lib 条目仍被 " + references + " 个测试集引用，无法删除");
        }
        repository.deleteById(id);
    }

    private static void applyEditableFields(SqlTemplate target, SqlTemplate source, boolean preserveMetadata) {
        if (source == null || !StringUtils.hasText(source.getName())) {
            throw new IllegalArgumentException("SQL Lib 名称不能为空");
        }
        if (!StringUtils.hasText(source.getSqlText())) {
            throw new IllegalArgumentException("SQL 内容不能为空");
        }

        int weight = source.getWeight() < 1 ? 1 : source.getWeight();
        String executionMode = normalizeExecutionMode(source.getExecutionMode());

        target.setName(source.getName().trim());
        target.setSqlText(source.getSqlText().trim());
        target.setWeight(weight);
        target.setDescription(trimToNull(source.getDescription()));
        target.setExecutionMode(executionMode);
        target.setParamJson("PREPARED_STATEMENT".equals(executionMode) ? trimToNull(source.getParamJson()) : null);
        if (!preserveMetadata) {
            target.setSourceFilename(trimToNull(source.getSourceFilename()));
            target.setUploadedAt(source.getUploadedAt() != null ? source.getUploadedAt() : Instant.now());
        }
    }

    private static String normalizeExecutionMode(String executionMode) {
        if (!StringUtils.hasText(executionMode)) {
            return "STATEMENT";
        }
        String normalized = executionMode.trim().toUpperCase(Locale.ROOT);
        if (!"STATEMENT".equals(normalized) && !"PREPARED_STATEMENT".equals(normalized)) {
            throw new IllegalArgumentException("不支持的执行模式: " + executionMode);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
