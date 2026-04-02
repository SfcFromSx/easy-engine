package com.smartbi.benchmark.sql;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlStatementSplitterTest {

    @Test
    void shouldSplitSqlWhileIgnoringSemicolonsInsideStringsAndComments() {
        String raw = ""
                + "SELECT ';' AS literal;\n"
                + "-- comment ; should not split\n"
                + "SELECT 2;\n"
                + "/* block ; comment */\n"
                + "SELECT 'a'';''b' AS quoted;";

        List<String> statements = SqlStatementSplitter.split(raw);

        assertEquals(3, statements.size());
        assertEquals("SELECT ';' AS literal", statements.get(0));
        assertEquals("-- comment ; should not split\nSELECT 2", statements.get(1));
        assertEquals("/* block ; comment */\nSELECT 'a'';''b' AS quoted", statements.get(2));
    }
}
