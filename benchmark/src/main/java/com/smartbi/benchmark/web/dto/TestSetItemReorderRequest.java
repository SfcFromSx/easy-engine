package com.smartbi.benchmark.web.dto;

import java.util.List;

public class TestSetItemReorderRequest {

    private List<Long> itemIds;

    public List<Long> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Long> itemIds) {
        this.itemIds = itemIds;
    }
}
