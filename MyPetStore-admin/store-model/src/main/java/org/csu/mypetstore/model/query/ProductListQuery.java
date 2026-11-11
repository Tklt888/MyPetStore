package org.csu.mypetstore.model.query;

import java.util.Set;

public class ProductListQuery {

    private static final Set<String> ALLOWED_STATUSES = Set.of("DRAFT", "ON_SALE");

    private Long page;
    private String keyword;
    private String status;
    private Long topCategoryId;
    private Long categoryId;

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTopCategoryId() { return topCategoryId; }
    public void setTopCategoryId(Long topCategoryId) { this.topCategoryId = topCategoryId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public long normalizedPage() {
        return page == null || page < 1 ? 1L : page;
    }

    public String normalizedKeyword() {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    public String normalizedStatus() {
        return status != null && ALLOWED_STATUSES.contains(status) ? status : null;
    }

    public Long normalizedTopCategoryId() { return topCategoryId != null && topCategoryId > 0 ? topCategoryId : null; }
    public Long normalizedCategoryId() { return categoryId != null && categoryId > 0 ? categoryId : null; }
}
