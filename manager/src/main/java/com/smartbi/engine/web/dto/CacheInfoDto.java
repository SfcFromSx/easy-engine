package com.smartbi.engine.web.dto;

public class CacheInfoDto {
    private String managedKeyPrefix;
    private boolean exactSummaryAvailable;
    private String summaryMessage;

    public String getManagedKeyPrefix() {
        return managedKeyPrefix;
    }

    public void setManagedKeyPrefix(String managedKeyPrefix) {
        this.managedKeyPrefix = managedKeyPrefix;
    }

    public boolean isExactSummaryAvailable() {
        return exactSummaryAvailable;
    }

    public void setExactSummaryAvailable(boolean exactSummaryAvailable) {
        this.exactSummaryAvailable = exactSummaryAvailable;
    }

    public String getSummaryMessage() {
        return summaryMessage;
    }

    public void setSummaryMessage(String summaryMessage) {
        this.summaryMessage = summaryMessage;
    }
}
