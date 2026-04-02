package com.smartbi.analyze.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoutingContext {
    private final String defaultDatasourceName;
    private final List<DatasourceDescriptor> datasources;
    private final List<AccelerationRule> accelerationRules;

    public RoutingContext(String defaultDatasourceName,
                          List<DatasourceDescriptor> datasources,
                          List<AccelerationRule> accelerationRules) {
        this.defaultDatasourceName = defaultDatasourceName;
        this.datasources = datasources == null
                ? Collections.<DatasourceDescriptor>emptyList()
                : Collections.unmodifiableList(new ArrayList<DatasourceDescriptor>(datasources));
        this.accelerationRules = accelerationRules == null
                ? Collections.<AccelerationRule>emptyList()
                : Collections.unmodifiableList(new ArrayList<AccelerationRule>(accelerationRules));
    }

    public String getDefaultDatasourceName() {
        if (hasText(defaultDatasourceName)) {
            return defaultDatasourceName;
        }
        for (DatasourceDescriptor datasource : datasources) {
            if (datasource != null && datasource.isDefault() && hasText(datasource.getName())) {
                return datasource.getName();
            }
        }
        return datasources.isEmpty() || datasources.get(0) == null ? null : datasources.get(0).getName();
    }

    public List<DatasourceDescriptor> getDatasources() {
        return datasources;
    }

    public List<AccelerationRule> getAccelerationRules() {
        return accelerationRules;
    }

    public DatasourceDescriptor findDatasource(String name) {
        if (!hasText(name)) {
            return null;
        }
        for (DatasourceDescriptor datasource : datasources) {
            if (datasource != null && name.trim().equals(datasource.getName())) {
                return datasource;
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
