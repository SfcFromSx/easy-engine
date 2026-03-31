package com.smartbi.benchmark.testset;

import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestSetImportServiceTest {

    // Covers TestSetImportService#importFromExcel header parsing, weight normalization, execution-mode normalization, and optional param_json.
    @Test
    void shouldImportSpreadsheetRowsIntoOrderedTestSetItems() throws Exception {
        BenchmarkTestSetRepository testSetRepository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        List<BenchmarkTestSetItem> savedItems = new ArrayList<BenchmarkTestSetItem>();
        TestSetImportService service = new TestSetImportService(testSetRepository, itemRepository);
        when(testSetRepository.save(any(BenchmarkTestSet.class))).thenAnswer(invocation -> {
            BenchmarkTestSet set = invocation.getArgument(0);
            ReflectionTestUtils.setField(set, "id", 12L);
            return set;
        });
        when(itemRepository.save(any(BenchmarkTestSetItem.class))).thenAnswer(invocation -> {
            BenchmarkTestSetItem item = invocation.getArgument(0);
            savedItems.add(item);
            return item;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cases.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes(true, true)
        );

        BenchmarkTestSet imported = service.importFromExcel(file, " Imported Cases ", " Imported from Excel ");

        assertEquals(Long.valueOf(12L), imported.getId());
        assertEquals("Imported Cases", imported.getName());
        assertEquals("Imported from Excel", imported.getDescription());
        assertEquals("cases.xlsx", imported.getSourceFilename());
        assertEquals(2, savedItems.size());
        assertEquals("first-row", savedItems.get(0).getLabel());
        assertEquals(3, savedItems.get(0).getWeight());
        assertEquals("PREPARED_STATEMENT", savedItems.get(0).getExecutionMode());
        assertEquals("[{\"type\":\"INTEGER\",\"value\":1}]", savedItems.get(0).getParamJson());
        assertEquals(1, savedItems.get(1).getWeight());
        assertEquals("STATEMENT", savedItems.get(1).getExecutionMode());
        assertNull(savedItems.get(1).getParamJson());
    }

    // Covers TestSetImportService#importFromExcel invalid extension and empty-row rejection.
    @Test
    void shouldRejectInvalidExtensionsAndEmptySheets() throws Exception {
        BenchmarkTestSetRepository testSetRepository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        TestSetImportService service = new TestSetImportService(testSetRepository, itemRepository);

        MockMultipartFile invalidExtension = new MockMultipartFile(
                "file",
                "cases.csv",
                "text/csv",
                "sql".getBytes(StandardCharsets.UTF_8)
        );
        IllegalArgumentException invalidType = assertThrows(IllegalArgumentException.class,
                () -> service.importFromExcel(invalidExtension, null, null));
        assertEquals("仅支持 .xlsx 或 .xls", invalidType.getMessage());

        MockMultipartFile emptyWorkbook = new MockMultipartFile(
                "file",
                "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes(false, false)
        );
        IllegalArgumentException emptySheet = assertThrows(IllegalArgumentException.class,
                () -> service.importFromExcel(emptyWorkbook, null, null));
        assertEquals("未解析到任何 SQL 行（请检查第一列为 SQL 文本）", emptySheet.getMessage());
    }

    // Covers TestSetImportService#importFromExcel invalid workbook parse failure propagation.
    @Test
    void shouldSurfaceWorkbookParsingErrors() {
        BenchmarkTestSetRepository testSetRepository = mock(BenchmarkTestSetRepository.class);
        BenchmarkTestSetItemRepository itemRepository = mock(BenchmarkTestSetItemRepository.class);
        TestSetImportService service = new TestSetImportService(testSetRepository, itemRepository);
        MockMultipartFile invalidWorkbook = new MockMultipartFile(
                "file",
                "broken.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "not-a-workbook".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(Exception.class, () -> service.importFromExcel(invalidWorkbook, null, null));
    }

    private static byte[] workbookBytes(boolean includeRows, boolean withHeader) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("cases");
            int rowIndex = 0;
            if (withHeader) {
                Row header = sheet.createRow(rowIndex++);
                header.createCell(0).setCellValue("sql");
                header.createCell(1).setCellValue("label");
                header.createCell(2).setCellValue("weight");
                header.createCell(3).setCellValue("execution_mode");
                header.createCell(4).setCellValue("param_json");
            }
            if (includeRows) {
                Row first = sheet.createRow(rowIndex++);
                first.createCell(0).setCellValue("SELECT * FROM SALES WHERE ID = ?");
                first.createCell(1).setCellValue("first-row");
                first.createCell(2).setCellValue("2.6");
                first.createCell(3).setCellValue("prepared_statement");
                first.createCell(4).setCellValue("[{\"type\":\"INTEGER\",\"value\":1}]");

                Row second = sheet.createRow(rowIndex++);
                second.createCell(0).setCellValue("SELECT * FROM SALES");
                second.createCell(1).setCellValue("second-row");
                second.createCell(2).setCellValue("0");

                Row blank = sheet.createRow(rowIndex);
                blank.createCell(0).setCellValue("");
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
