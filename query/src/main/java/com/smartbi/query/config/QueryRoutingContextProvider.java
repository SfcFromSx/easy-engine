package com.smartbi.query.config;

import com.smartbi.analyze.route.AccelerationRule;
import com.smartbi.analyze.route.DatasourceDescriptor;
import com.smartbi.analyze.route.RoutingContext;
import com.smartbi.query.datasource.ManagedDataSourceRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class QueryRoutingContextProvider {

    private static final long REFRESH_INTERVAL_MS = 30000L;

    private final ManagedDataSourceRegistry managedDataSourceRegistry;
    private final ManagerConfigClient managerConfigClient;

    private volatile RoutingContext cachedContext;
    private volatile long lastRefreshAt;

    public QueryRoutingContextProvider(ManagedDataSourceRegistry managedDataSourceRegistry,
                                       ManagerConfigClient managerConfigClient) {
        this.managedDataSourceRegistry = managedDataSourceRegistry;
        this.managerConfigClient = managerConfigClient;
    }

    public RoutingContext getRoutingContext() {
        long now = System.currentTimeMillis();
        RoutingContext current = cachedContext;
        if (current != null && now - lastRefreshAt < REFRESH_INTERVAL_MS) {
            return current;
        }
        synchronized (this) {
            current = cachedContext;
            if (current != null && now - lastRefreshAt < REFRESH_INTERVAL_MS) {
                return current;
            }
            try {
                ManagerConfigClient.ManagerRoutingContext managerContext = managerConfigClient.fetchRoutingContext();
                RoutingContext refreshed = new RoutingContext(
                        managedDataSourceRegistry.getDefaultName(),
                        managedDataSourceRegistry.snapshotDescriptors(),
                        toAccelerationRules(managerContext == null ? null : managerContext.getAccelerationRules())
                );
                cachedContext = refreshed;
                lastRefreshAt = now;
                return refreshed;
            } catch (Exception ex) {
                RoutingContext fallback = current != null ? current : new RoutingContext(
                        managedDataSourceRegistry.getDefaultName(),
                        managedDataSourceRegistry.snapshotDescriptors(),
                        Collections.<AccelerationRule>emptyList()
                );
                cachedContext = fallback;
                lastRefreshAt = now;
                return fallback;
            }
        }
    }

    private static List<AccelerationRule> toAccelerationRules(List<ManagerConfigClient.ManagerAccelerationRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        List<AccelerationRule> converted = new ArrayList<AccelerationRule>(rules.size());
        for (ManagerConfigClient.ManagerAccelerationRule rule : rules) {
            if (rule == null) {
                continue;
            }
            converted.add(new AccelerationRule(
                    rule.getName(),
                    rule.getSchemaName(),
                    rule.getTableName(),
                    rule.getRefreshSql()
            ));
        }
        return converted;
    }
}
