package org.csu.mypetstore.model.query;

import java.util.Locale;
import java.util.Set;

public class CustomerListQuery {
    private static final Set<String> STATUSES = Set.of("ENABLED", "DISABLED");
    private String keyword;
    private String status;
    private Long page;
    private Long pageSize;
    private String normalizedKeywordPattern;
    private String normalizedStatus;
    private long normalizedPage;
    private long normalizedPageSize;

    public void normalize() {
        normalizedKeywordPattern = normalizeKeyword(keyword);
        normalizedStatus = normalizeStatus(status);
        normalizedPage = page == null || page < 1 ? 1L : page;
        normalizedPageSize = pageSize == null ? 10L : pageSize;
        if (normalizedPageSize < 1 || normalizedPageSize > 50) {
            throw new IllegalArgumentException("每页条数必须在1到50之间");
        }
    }

    private String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.strip();
        if (trimmed.length() > 50) throw new IllegalArgumentException("用户名搜索不能超过50个字符");
        return trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.strip())) return null;
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) throw new IllegalArgumentException("用户状态不合法");
        return normalized;
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getPage() { return page; }
    public void setPage(Long page) { this.page = page; }
    public Long getPageSize() { return pageSize; }
    public void setPageSize(Long pageSize) { this.pageSize = pageSize; }
    public String normalizedKeywordPattern() { return normalizedKeywordPattern; }
    public String normalizedStatus() { return normalizedStatus; }
    public long normalizedPage() { return normalizedPage; }
    public long normalizedPageSize() { return normalizedPageSize; }
}
