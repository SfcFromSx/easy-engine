package com.smartbi.engine.parse;

import com.smartbi.engine.domain.ParseStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlParseServiceTest {

    private final SqlParseService service = new SqlParseService();

    @Test
    void parsesSimpleSelect() {
        ParseOutcome out = service.analyze("SELECT a, count(*) FROM t1 GROUP BY a");
        assertEquals(ParseStatus.OK, out.getStatus());
        assertNotNull(out.getSignature());
        assertFalse(out.getSignature().getTables().isEmpty());
        assertFalse(out.getSignature().getSelectItems().isEmpty());
    }

    @Test
    void skippedOnEmpty() {
        ParseOutcome out = service.analyze("   ");
        assertEquals(ParseStatus.SKIPPED, out.getStatus());
    }
}
