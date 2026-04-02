package com.smartbi.benchmark.sql;

import com.smartbi.benchmark.domain.SqlTemplate;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlLibImportServiceTest {

    private final SqlLibImportService service = new SqlLibImportService();

    @Test
    void shouldImportWorkbookAndExcelCompatibleEtFiles() throws Exception {
        MockMultipartFile workbook = new MockMultipartFile(
                "file",
                "cases.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes()
        );
        MockMultipartFile etWorkbook = new MockMultipartFile(
                "file",
                "cases.et",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes()
        );

        List<SqlTemplate> workbookRows = service.importFromFile(workbook);
        List<SqlTemplate> etRows = service.importFromFile(etWorkbook);

        assertEquals(2, workbookRows.size());
        assertEquals("Named SQL", workbookRows.get(0).getName());
        assertEquals("PREPARED_STATEMENT", workbookRows.get(0).getExecutionMode());
        assertEquals(3, workbookRows.get(0).getWeight());
        assertEquals("imported description", workbookRows.get(0).getDescription());
        assertEquals("[{\"type\":\"INTEGER\",\"value\":1}]", workbookRows.get(0).getParamJson());
        assertEquals("Label Only", workbookRows.get(1).getName());
        assertEquals("STATEMENT", etRows.get(1).getExecutionMode());
    }

    @Test
    void shouldImportCsvRowsAndSqlTextFiles() throws Exception {
        MockMultipartFile csv = new MockMultipartFile(
                "file",
                "cases.csv",
                "text/csv",
                ("sql,name,weight,execution_mode,param_json,description\n"
                        + "\"SELECT * FROM SALES WHERE ID = ?\",CSV SQL,2,PREPARED_STATEMENT,\"[{\"\"type\"\":\"\"INTEGER\"\",\"\"value\"\":1}]\",csv import\n")
                        .getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile sql = new MockMultipartFile(
                "file",
                "bundle.sql",
                "text/plain",
                ("SELECT 1;\n"
                        + "-- keep ; in comment\n"
                        + "SELECT ';' AS text;\n")
                        .getBytes(StandardCharsets.UTF_8)
        );

        List<SqlTemplate> csvRows = service.importFromFile(csv);
        List<SqlTemplate> sqlRows = service.importFromFile(sql);

        assertEquals(1, csvRows.size());
        assertEquals("CSV SQL", csvRows.get(0).getName());
        assertEquals("PREPARED_STATEMENT", csvRows.get(0).getExecutionMode());
        assertEquals(2, csvRows.get(0).getWeight());

        assertEquals(2, sqlRows.size());
        assertEquals("bundle #1", sqlRows.get(0).getName());
        assertEquals("bundle #2", sqlRows.get(1).getName());
    }

    @Test
    void shouldRejectUnsupportedExtensionsAndEmptyImports() {
        MockMultipartFile invalid = new MockMultipartFile(
                "file",
                "cases.md",
                "text/plain",
                "select 1".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile emptySql = new MockMultipartFile(
                "file",
                "blank.sql",
                "text/plain",
                " ; -- no statements".getBytes(StandardCharsets.UTF_8)
        );

        IllegalArgumentException invalidType = assertThrows(IllegalArgumentException.class,
                () -> service.importFromFile(invalid));
        assertEquals("仅支持 .xlsx、.xls、.et、.csv、.txt 或 .sql", invalidType.getMessage());

        IllegalArgumentException emptyImport = assertThrows(IllegalArgumentException.class,
                () -> service.importFromFile(emptySql));
        assertEquals("未解析到任何 SQL 行", emptyImport.getMessage());
    }

    private static byte[] workbookBytes() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("sql-lib");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("sql");
            header.createCell(1).setCellValue("name");
            header.createCell(2).setCellValue("weight");
            header.createCell(3).setCellValue("execution_mode");
            header.createCell(4).setCellValue("param_json");
            header.createCell(5).setCellValue("description");

            Row first = sheet.createRow(1);
            first.createCell(0).setCellValue("SELECT * FROM SALES WHERE ID = ?");
            first.createCell(1).setCellValue("Named SQL");
            first.createCell(2).setCellValue("3");
            first.createCell(3).setCellValue("PREPARED_STATEMENT");
            first.createCell(4).setCellValue("[{\"type\":\"INTEGER\",\"value\":1}]");
            first.createCell(5).setCellValue("imported description");

            Row second = sheet.createRow(2);
            second.createCell(0).setCellValue("SELECT 1");
            second.createCell(1).setCellValue("Label Only");
            second.createCell(2).setCellValue("1");
            second.createCell(3).setCellValue("STATEMENT");

            workbook.write(output);
            return output.toByteArray();
        }
    }
}
