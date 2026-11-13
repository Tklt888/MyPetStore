package org.csu.mypetstore.model.query;

import java.util.Locale;
import java.util.Set;

public class OrderListQuery {
    private static final Set<String> STATUSES = Set.of(
            "PENDING_PAYMENT", "PAID", "SHIPPED", "COMPLETED", "CANCELLED");

    private String orderNo;
    private String status;
    private Long page;
    private Long pageSize;
    private String normalizedOrderNo;
    private String normalizedStatus;
    private long normalizedPage;
    private long normalizedPageSize;

    public void normalize() {
        normalizedOrderNo = normalizeOrderNo(orderNo);
        normalizedStatus = normalizeStatus(status);
        normalizedPage = page == null || page < 1 ? 1L : page;
        normalizedPageSize = pageSize == null ? 10L : pageSize;
        if (normalizedPageSize < 1 || normalizedPageSize > 50) {
            throw new IllegalArgumentException("每页条数必须在1到50之间");
        }
    }

    private String normalizeOrderNo(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > 40) throw new IllegalArgumentException("订单号不能超过40个字符");
        return trimmed;
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim())) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) throw new IllegalArgumentException("订单状态不合法");
        return normalized;
    }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getPage() { return page; }
    public void setPage(Long page) { this.page = page; }
    public Long getPageSize() { return pageSize; }
    public void setPageSize(Long pageSize) { this.pageSize = pageSize; }
    public String normalizedOrderNo() { return normalizedOrderNo; }
    public String normalizedStatus() { return normalizedStatus; }
    public long normalizedPage() { return normalizedPage; }
    public long normalizedPageSize() { return normalizedPageSize; }
}
