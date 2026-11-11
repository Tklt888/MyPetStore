package org.csu.mypetstore.admin.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductUpdateForm {
    @NotNull private Long categoryId;
    @NotBlank @Size(max = 120) private String name;
    @Size(max = 500) private String summary;
    private String description;
    @NotBlank @Size(max = 255) private String coverImage;
    @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) private BigDecimal price;
    @NotNull @Min(0) private Integer sortOrder;
    @NotNull @Min(0) private Long version;

    public Long getCategoryId() { return categoryId; } public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getSummary() { return summary; } public void setSummary(String summary) { this.summary = summary; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public String getCoverImage() { return coverImage; } public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public BigDecimal getPrice() { return price; } public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getSortOrder() { return sortOrder; } public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Long getVersion() { return version; } public void setVersion(Long version) { this.version = version; }
}
