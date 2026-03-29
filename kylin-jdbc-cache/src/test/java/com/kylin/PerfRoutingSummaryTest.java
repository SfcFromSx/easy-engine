package com.kylin;

import com.kylin.cache.RedisCacheManager;
import com.kylin.datasource.DataSourceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 专项测试：全面覆盖 Statement 和 PreparedStatement (带/不带参数) 的路由统计与性能对比。
 */
public class PerfRoutingSummaryTest {

    private Connection virtualConn;
    private RedisCacheManager redisCache;
    private DriverConfig config;
    
    // 计数器
    private final AtomicInteger physicalCalls = new AtomicInteger(0);
    private final AtomicInteger kylinCalls = new AtomicInteger(0);
    private final AtomicInteger prestoCalls = new AtomicInteger(0);

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.setProperty("datasource.names", "kylin_default,presto_1");
        props.setProperty("datasource.default", "kylin_default");
        props.setProperty("datasource.kylin_default.type", "kylin");
        props.setProperty("datasource.kylin_default.driver_class", "org.apache.kylin.jdbc.Driver");
        props.setProperty("datasource.presto_1.type", "presto");
        props.setProperty("datasource.presto_1.driver_class", "org.apache.kylin.jdbc.Driver");
        
        config = DriverConfig.parse("jdbc:kylin-cached://localhost/perf_test", props);
        DataSourceRegistry spyRegistry = Mockito.spy(config.getDataSourceRegistry());
        
        // --- Mock Kylin 物理执行 (100ms) ---
        Connection mkKylin = mock(Connection.class);
        Statement mkKylinStmt = mock(Statement.class);
        PreparedStatement mkKylinPS = mock(PreparedStatement.class);
        
        when(mkKylin.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(mkKylinStmt);
        when(mkKylin.prepareStatement(anyString())).thenReturn(mkKylinPS);
        when(mkKylin.prepareStatement(anyString(), anyInt())).thenReturn(mkKylinPS);
        when(mkKylin.prepareStatement(anyString(), any(int[].class))).thenReturn(mkKylinPS);
        when(mkKylin.prepareStatement(anyString(), any(String[].class))).thenReturn(mkKylinPS);
        when(mkKylin.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(mkKylinPS);
        when(mkKylin.prepareStatement(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(mkKylinPS);
        
        doAnswer(inv -> simulatePhysical("kylin_stmt")).when(mkKylinStmt).executeQuery(anyString());
        doAnswer(inv -> simulatePhysical("kylin_ps")).when(mkKylinPS).executeQuery();
        doAnswer(inv -> simulatePhysical("kylin_ps")).when(mkKylinPS).executeQuery(anyString());

        // --- Mock Presto 物理执行 (150ms) ---
        Connection mkPresto = mock(Connection.class);
        Statement mkPrestoStmt = mock(Statement.class);
        PreparedStatement mkPrestoPS = mock(PreparedStatement.class);
        
        when(mkPresto.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(mkPrestoStmt);
        when(mkPresto.prepareStatement(anyString())).thenReturn(mkPrestoPS);
        when(mkPresto.prepareStatement(anyString(), anyInt())).thenReturn(mkPrestoPS);
        when(mkPresto.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(mkPrestoPS);
        when(mkPresto.prepareStatement(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(mkPrestoPS);
        
        doAnswer(inv -> simulatePhysical("presto_stmt")).when(mkPrestoStmt).executeQuery(anyString());
        doAnswer(inv -> simulatePhysical("presto_ps")).when(mkPrestoPS).executeQuery();
        doAnswer(inv -> simulatePhysical("presto_ps")).when(mkPrestoPS).executeQuery(anyString());

        // 绑定到 spyRegistry
        doReturn(mkKylin).when(spyRegistry).getConnection("kylin_default");
        doReturn(mkPresto).when(spyRegistry).getConnection("presto_1");
        
        config.setDataSourceRegistry(spyRegistry);
        redisCache = RedisCacheManager.getInstance(config);
        redisCache.flushAll();
        virtualConn = new CachedKylinConnection(config, redisCache);
    }

    private ResultSet simulatePhysical(String tag) throws Exception {
        physicalCalls.incrementAndGet();
        if (tag.contains("kylin")) kylinCalls.incrementAndGet();
        else prestoCalls.incrementAndGet();
        Thread.sleep(tag.contains("presto") ? 150 : 100);
        return mockResultSet(tag + "_result");
    }

    private ResultSet mockResultSet(String val) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        java.sql.ResultSetMetaData meta = mock(java.sql.ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(1);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(1)).thenReturn(val);
        return rs;
    }

    @Test
    public void runCoverageAndPerformanceTrial() throws Exception {
        System.out.println("\n========== 引擎路由及语句类型覆盖性报告 ==========\n");

        // --- KYLIN 路由测试 ---
        testSuite("kylin_default", "Kylin");

        // --- PRESTO 路由测试 ---
        testSuite("presto_1", "Presto");

        System.out.println("\n--- 汇总统计 ---");
        System.out.println("总物理执行次数: " + physicalCalls.get());
        System.out.println("Kylin 物理分发次数: " + kylinCalls.get());
        System.out.println("Presto 物理分发次数: " + prestoCalls.get());
        
        // 期望：每个数据源 3 种方式各执行一次物理查询（共 6 次），缓存命中不计入。
        assertEquals(3, kylinCalls.get());
        assertEquals(3, prestoCalls.get());

        System.out.println("\n================================================\n");
    }

    private void testSuite(String engine, String label) throws Exception {
        String hint = "/* engine=" + engine + " */ ";
        
        // 1. Statement (No ?)
        measure(label + " [Statement-NoParam]", () -> 
            virtualConn.createStatement().executeQuery(hint + "SELECT 1"));

        // 2. PreparedStatement (No ?)
        measure(label + " [PreparedStmt-NoParam]", () -> 
            virtualConn.prepareStatement(hint + "SELECT 2").executeQuery());

        // 3. PreparedStatement (With ?)
        measure(label + " [PreparedStmt-WithParam]", () -> {
            PreparedStatement ps = virtualConn.prepareStatement(hint + "SELECT 3 WHERE id = ?");
            ps.setInt(1, 100);
            return ps.executeQuery();
        });
    }

    private void measure(String task, QueryRunner runner) throws Exception {
        // 第一轮：Miss
        long s1 = System.currentTimeMillis();
        runner.run();
        long d1 = System.currentTimeMillis() - s1;

        // 第二轮：Hit
        long s2 = System.currentTimeMillis();
        runner.run();
        long d2 = System.currentTimeMillis() - s2;

        System.out.printf("%-30s | Miss: %3d ms | Hit: %2d ms\n", task, d1, d2);
        assertTrue("缓存应有效加速", d2 < d1);
    }

    @FunctionalInterface
    interface QueryRunner {
        ResultSet run() throws Exception;
    }

    @After
    public void tearDown() throws Exception {
        if (virtualConn != null) virtualConn.close();
    }
}
