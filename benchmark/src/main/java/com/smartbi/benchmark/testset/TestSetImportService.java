package com.smartbi.benchmark.testset;

import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TestSetImportService {

    private final BenchmarkTestSetRepository testSetRepository;
    private final BenchmarkTestSetItemRepository itemRepository;

    public TestSetImportService(BenchmarkTestSetRepository testSetRepository,
                                BenchmarkTestSetItemRepository itemRepository) {
        this.testSetRepository = testSetRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public BenchmarkTestSet importFromExcel(MultipartFile file, String nameOverride) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        String orig = file.getOriginalFilename();
        if (orig == null) {
            throw new IllegalArgumentException("无法识别文件名");
        }
        String lower = orig.toLowerCase();
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw new IllegalArgumentException("仅支持 .xlsx 或 .xls");
        }

        List<ParsedRow> rows;
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel 无工作表");
            }
            rows = parseSheet(sheet);
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("未解析到任何 SQL 行（请检查第一列为 SQL 文本）");
        }

        BenchmarkTestSet set = new BenchmarkTestSet();
        set.setName(StringUtils.hasText(nameOverride) ? nameOverride.trim() : stripExtension(orig));
        set.setSourceFilename(orig);
        set = testSetRepository.save(set);

        int order = 0;
        for (ParsedRow r : rows) {
            BenchmarkTestSetItem it = new BenchmarkTestSetItem();
            it.setTestSetId(set.getId());
            it.setSortOrder(order++);
            it.setLabel(r.label);
            it.setSqlText(r.sql);
            it.setWeight(r.weight);
            it.setExecutionMode(r.executionMode);
            it.setParamJson(r.paramJson);
            itemRepository.save(it);
        }
        return set;
    }

    private static List<ParsedRow> parseSheet(Sheet sheet) {
        DataFormatter fmt = new DataFormatter();
        List<ParsedRow> out = new ArrayList<>();
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        if (lastRow < firstRow) {
            return out;
        }

        Row headerCandidate = sheet.getRow(firstRow);
        int dataStart = firstRow;
        Map<String, Integer> headerMap = null;
        if (headerCandidate != null) {
            String h = cellString(headerCandidate.getCell(0), fmt);
            if (isHeaderRow(h)) {
                dataStart = firstRow + 1;
                headerMap = parseHeaderMap(headerCandidate, fmt);
            }
        }

        for (int i = dataStart; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String sql = cellString(cell(row, headerMap, "sql", 0), fmt);
            if (!StringUtils.hasText(sql)) {
                continue;
            }
            String label = cellString(cell(row, headerMap, "label", 1), fmt);
            int weight = 1;
            String w = cellString(cell(row, headerMap, "weight", 2), fmt);
            if (StringUtils.hasText(w)) {
                try {
                    double d = Double.parseDouble(w.trim());
                    weight = (int) Math.round(d);
                } catch (NumberFormatException ignored) {
                    weight = 1;
                }
                if (weight < 1) {
                    weight = 1;
                }
            }
            ParsedRow pr = new ParsedRow();
            pr.sql = sql.trim();
            pr.label = StringUtils.hasText(label) ? label.trim() : null;
            pr.weight = weight;
            String executionMode = cellString(cell(row, headerMap, "execution_mode", 3), fmt);
            pr.executionMode = StringUtils.hasText(executionMode) ? executionMode.trim().toUpperCase(Locale.ROOT) : "STATEMENT";
            String paramJson = cellString(cell(row, headerMap, "param_json", 4), fmt);
            pr.paramJson = StringUtils.hasText(paramJson) ? paramJson.trim() : null;
            out.add(pr);
        }
        return out;
    }

    private static Map<String, Integer> parseHeaderMap(Row row, DataFormatter fmt) {
        Map<String, Integer> out = new HashMap<>();
        short last = row.getLastCellNum();
        for (int i = 0; i < last; i++) {
            String value = cellString(row.getCell(i), fmt);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            out.put(value.trim().toLowerCase(Locale.ROOT), i);
        }
        return out;
    }

    private static Cell cell(Row row, Map<String, Integer> headerMap, String key, int fallbackIndex) {
        if (headerMap != null) {
            Integer idx = headerMap.get(key);
            if (idx != null) {
                return row.getCell(idx);
            }
        }
        return row.getCell(fallbackIndex);
    }

    private static boolean isHeaderRow(String c0) {
        if (c0 == null) {
            return false;
        }
        String s = c0.trim().toLowerCase();
        return s.equals("sql")
                || s.equals("语句")
                || s.endsWith("sql")
                || s.contains("sql文本")
                || s.contains("sql语句");
    }

    private static String cellString(Cell cell, DataFormatter fmt) {
        if (cell == null) {
            return "";
        }
        return fmt.formatCellValue(cell).trim();
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static final class ParsedRow {
        String sql;
        String label;
        int weight = 1;
        String executionMode = "STATEMENT";
        String paramJson;
    }
}
