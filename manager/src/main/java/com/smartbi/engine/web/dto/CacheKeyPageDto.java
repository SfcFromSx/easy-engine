package com.smartbi.engine.web.dto;

import java.util.ArrayList;
import java.util.List;

public class CacheKeyPageDto {
    private List<CacheKeyDto> items = new ArrayList<CacheKeyDto>();
    private String nextCursor;
    private boolean hasMore;
    private String queryPrefix;

    public List<CacheKeyDto> getItems() {
        return items;
    }

    public void setItems(List<CacheKeyDto> items) {
        this.items = items;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public String getQueryPrefix() {
        return queryPrefix;
    }

    public void setQueryPrefix(String queryPrefix) {
        this.queryPrefix = queryPrefix;
    }
}
