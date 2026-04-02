package com.smartbi.analyze.route;

public class DatasourceDescriptor {
    private final String name;
    private final String type;
    private final boolean isDefault;

    public DatasourceDescriptor(String name, String type, boolean isDefault) {
        this.name = name;
        this.type = type;
        this.isDefault = isDefault;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
