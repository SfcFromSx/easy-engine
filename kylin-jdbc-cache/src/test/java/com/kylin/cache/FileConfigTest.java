package com.kylin.cache;

import com.kylin.DriverConfig;
import org.junit.Test;
import java.util.Properties;
import static org.junit.Assert.*;

public class FileConfigTest {

    @Test
    public void testFileLoading() throws Exception {
        // 使用一个极简 URL，不带任何参数
        String url = "jdbc:kylin-cached://localhost/proj";
        DriverConfig config = DriverConfig.parse(url, new Properties());
        
        // 验证是否加载了 kylin-cache.properties 中的默认值 (2MB = 2097152)
        assertEquals(2097152, config.getMaxCacheSizeBytes());
        assertEquals("kylin_cache:", config.getCacheKeyPrefix());
        assertEquals(300, config.getDefaultTtlSeconds());
        assertFalse(config.isConservativeCacheModeEnabled());
        assertTrue(config.isPreparedStatementCacheEnabled());
        assertFalse(config.isSqlTraceEnabled());
        assertEquals(512, config.getSqlTraceMaxSqlLength());
        
        System.out.println("External file configuration loading passed!");
    }
}
