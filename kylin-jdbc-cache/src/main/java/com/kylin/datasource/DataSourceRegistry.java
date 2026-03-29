package com.kylin.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源注册中心。根据 DriverConfig 中定义的实例提供底层连接池策略。
 * 
 * <p>变更记录：使用 Lazy Loading (延迟加载) 策略载入驱动和连接池，
 * 防止在单元测试阶段因缺少某些特定数据库驱动类（如 Presto/H2）而导致配置解析整体崩溃。
 */
public class DataSourceRegistry implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DataSourceRegistry.class);

    private final Map<String, ConnectionProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, DataSourceConfig> configs = new ConcurrentHashMap<>();
    private String defaultDataSourceName;
    private volatile boolean closed = false;

    /**
     * 仅注册配置。实际的 ConnectionProvider (及驱动加载) 将在首次获取连接时完成。
     */
    public void register(DataSourceConfig config) {
        if (closed || config == null) return;
        configs.put(config.getName(), config);
        log.debug("已登记数据源配置 [{}] (type={})", config.getName(), config.getType());
    }

    public void setDefaultDataSourceName(String defaultDataSourceName) {
        this.defaultDataSourceName = defaultDataSourceName;
    }

    public String getDefaultDataSourceName() {
        return defaultDataSourceName;
    }

    /**
     * 获取所有已注册的数据源名称集合。
     */
    public java.util.Set<String> getDataSourceNames() {
        return configs.keySet();
    }

    /**
     * 按名称获取对应数据源的连接。若未找到则回退到默认数据源。
     */
    public Connection getConnection(String name) throws SQLException {
        if (closed) {
            throw new SQLException("数据源注册表已关闭 (closed)");
        }

        ConnectionProvider provider = getOrCreateProvider(name);
        if (provider == null) {
            log.warn("无法找到名为 '{}' 的数据源，尝试回退到默认数据源: '{}'", name, defaultDataSourceName);
            provider = getOrCreateProvider(defaultDataSourceName);
        }
        
        if (provider == null) {
            throw new SQLException("无法取得目标数据源连接: " + name + " (未找到/not found，且默认数据源同样不存在)");
        }
        return provider.getConnection();
    }

    private ConnectionProvider getOrCreateProvider(String name) throws SQLException {
        if (name == null || closed) return null;
        
        ConnectionProvider provider = providers.get(name);
        if (provider != null) return provider;

        // 加锁确保线程安全地初始化
        synchronized (providers) {
            if (closed) return null;
            provider = providers.get(name);
            if (provider != null) return provider;

            DataSourceConfig config = configs.get(name);
            if (config == null) return null;

            try {
                if (config.isPoolEnabled()) {
                    provider = new HikariConnectionProvider(config);
                } else {
                    provider = new SingleConnectionProvider(config);
                }
                providers.put(name, provider);
                log.info("成功初始化数据源实例 [{}] (type={}, driver={})", 
                        config.getName(), config.getType(), config.getDriverClass());
                return provider;
            } catch (Throwable t) {
                log.error("初始化数据源实例 [{}] 失败: {}", name, t.getMessage());
                throw new SQLException("驱动加载或数据源池初始化失败: " + name, t);
            }
        }
    }
    
    /**
     * 按名称获取数据源配置特征。若未找到则回退到默认数据源配置。
     */
    public DataSourceConfig getConfig(String name) {
        DataSourceConfig config = configs.get(name);
        if (config == null && defaultDataSourceName != null) {
            config = configs.get(defaultDataSourceName);
        }
        return config;
    }

    @Override
    public void close() throws Exception {
        synchronized (providers) {
            if (closed) return;
            closed = true;
            for (Map.Entry<String, ConnectionProvider> entry : providers.entrySet()) {
                try {
                    entry.getValue().close();
                    log.info("已卸载数据源环境: {}", entry.getKey());
                } catch (Exception e) {
                    log.error("卸载数据源 {} 时报错", entry.getKey(), e);
                }
            }
            providers.clear();
            configs.clear();
        }
    }
}
