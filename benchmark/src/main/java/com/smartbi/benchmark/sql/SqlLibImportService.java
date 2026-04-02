package com.smartbi.benchmark.sql;

import com.smartbi.benchmark.domain.SqlTemplate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SqlLibImportService {

    public List<SqlTemplate> importFromFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("无法识别文件名");
        }

        String lower = originalFilename.toLowerCase(Locale.ROOT);
        List<ParsedRow> rows;
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".et")) {
            rows = parseWorkbook(file);
        } else if (lower.endsWith(".csv")) {
            rows = parseCsv(file);
        } else if (lower.endsWith(".txt") || lower.endsWith(".sql")) {
            rows = parseStatements(file, stripExtension(originalFilename));
        } else {
            throw new IllegalArgumentException("仅支持 .xlsx、.xls、.et、.csv、.txt 或 .sql");
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("未解析到任何 SQL 行");
        }

        String baseFilename = stripExtension(originalFilename);
        List<SqlTemplate> templates = new ArrayList<SqlTemplate>();
        for (int i = 0; i < rows.size(); i++) {
            ParsedRow row = rows.get(i);
            SqlTemplate template = new SqlTemplate();
            template.setName(resolveName(row, baseFilename, i + 1));
            template.setSqlText(row.sql);
            template.setWeight(row.weight);
            template.setDescription(row.description);
            template.setExecutionMode(row.executionMode);
            template.setParamJson("PREPARED_STATEMENT".equals(row.executionMode) ? row.paramJson : null);
            templates.add(template);
        }
        return templates;
    }

    private static List<ParsedRow> parseWorkbook(MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Excel 无工作表");
            }

            DataFormatter formatter = new DataFormatter();
            List<List<String>> rows = new ArrayList<List<String>>();
            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                List<String> values = new ArrayList<String>();
                short lastCell = row.getLastCellNum();
                for (int cellIndex = 0; cellIndex < Math.max(0, lastCell); cellIndex++) {
                    Cell cell = row.getCell(cellIndex);
                    values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                }
                rows.add(values);
            }
            return parseTabularRows(rows);
        }
    }

    private static List<ParsedRow> parseCsv(MultipartFile file) throws Exception {
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {
            List<List<String>> rows = new ArrayList<List<String>>();
            for (CSVRecord record : parser) {
                List<String> values = new ArrayList<String>();
                for (String value : record) {
                    values.add(value == null ? "" : value.trim());
                }
                rows.add(values);
            }
            return parseTabularRows(rows);
        }
    }

    private static List<ParsedRow> parseStatements(MultipartFile file, String baseFilename) throws Exception {
        String raw;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
            raw = builder.toString();
        }
        List<String> statements = SqlStatementSplitter.split(raw);
        List<ParsedRow> rows = new ArrayList<ParsedRow>();
        for (int i = 0; i < statements.size(); i++) {
            ParsedRow row = new ParsedRow();
            row.name = baseFilename + " #" + (i + 1);
            row.sql = statements.get(i);
            rows.add(row);
        }
        return rows;
    }

    private static List<ParsedRow> parseTabularRows(List<List<String>> rows) {
        List<ParsedRow> parsedRows = new ArrayList<ParsedRow>();
        if (rows.isEmpty()) {
            return parsedRows;
        }

        int startIndex = 0;
        Map<String, Integer> headerMap = null;
        if (isHeaderRow(rows.get(0))) {
            headerMap = toHeaderMap(rows.get(0));
            startIndex = 1;
        }

        for (int index = startIndex; index < rows.size(); index++) {
            List<String> values = rows.get(index);
            String sql = valueAt(values, headerMap, "sql", 0);
            if (!StringUtils.hasText(sql)) {
                continue;
            }

            ParsedRow row = new ParsedRow();
            row.name = valueAt(values, headerMap, "name", -1);
            row.label = valueAt(values, headerMap, "label", 1);
            row.description = emptyToNull(valueAt(values, headerMap, "description", 5));
            row.sql = sql.trim();
            row.weight = parseWeight(valueAt(values, headerMap, "weight", 2));
            row.executionMode = normalizeExecutionMode(valueAt(values, headerMap, "execution_mode", 3));
            row.paramJson = emptyToNull(valueAt(values, headerMap, "param_json", 4));
            parsedRows.add(row);
        }

        return parsedRows;
    }

    private static boolean isHeaderRow(List<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (String value : values) {
            String normalized = normalizeHeader(value);
            if ("sql".equals(normalized)
                    || "name".equals(normalized)
                    || "label".equals(normalized)
                    || "weight".equals(normalized)
                    || "execution_mode".equals(normalized)
                    || "param_json".equals(normalized)
                    || "description".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Integer> toHeaderMap(List<String> row) {
        Map<String, Integer> headerMap = new HashMap<String, Integer>();
        for (int i = 0; i < row.size(); i++) {
            String header = normalizeHeader(row.get(i));
            if (StringUtils.hasText(header)) {
                headerMap.put(header, i);
            }
        }
        return headerMap;
    }

    private static String normalizeHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if ("sql_text".equals(normalized)) {
            return "sql";
        }
        return normalized;
    }

    private static String valueAt(List<String> values, Map<String, Integer> headerMap, String key, int fallbackIndex) {
        if (headerMap != null && headerMap.containsKey(key)) {
            int index = headerMap.get(key);
            return index >= 0 && index < values.size() ? values.get(index) : "";
        }
        if (fallbackIndex >= 0 && fallbackIndex < values.size()) {
            return values.get(fallbackIndex);
        }
        return "";
    }

    private static int parseWeight(String raw) {
        if (!StringUtils.hasText(raw)) {
            return 1;
        }
        try {
            int parsed = (int) Math.round(Double.parseDouble(raw.trim()));
            return parsed < 1 ? 1 : parsed;
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private static String normalizeExecutionMode(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "STATEMENT";
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!"STATEMENT".equals(normalized) && !"PREPARED_STATEMENT".equals(normalized)) {
            throw new IllegalArgumentException("不支持的执行模式: " + raw);
        }
        return normalized;
    }

    private static String resolveName(ParsedRow row, String baseFilename, int index) {
        if (StringUtils.hasText(row.name)) {
            return row.name.trim();
        }
        if (StringUtils.hasText(row.label)) {
            return row.label.trim();
        }
        return baseFilename + " #" + index;
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static final class ParsedRow {
        String name;
        String label;
        String description;
        String sql;
        int weight = 1;
        String executionMode = "STATEMENT";
        String paramJson;
    }
}
