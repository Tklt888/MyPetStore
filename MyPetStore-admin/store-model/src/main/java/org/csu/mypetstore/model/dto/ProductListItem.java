package org.csu.mypetstore.model.dto;

import java.math.BigDecimal;

public class ProductListItem {

    private Long id;
    private String sku;
    private String name;
    private String categoryName;
    private Long categoryId;
    private String topCategoryName;
    private Long topCategoryId;
    private BigDecimal price;
    private Integer stock;
    private String status;
    private Integer sortOrder;
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTopCategoryName() { return topCategoryName; }
    public void setTopCategoryName(String topCategoryName) { this.topCategoryName = topCategoryName; }
    public Long getTopCategoryId() { return topCategoryId; }
    public void setTopCategoryId(Long topCategoryId) { this.topCategoryId = topCategoryId; }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
