package org.csu.mypetstore.admin.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ProductRestockForm {
    @NotNull @Min(1) private Integer quantity;
    @NotNull @Min(0) private Long version;
    public Integer getQuantity() { return quantity; } public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getVersion() { return version; } public void setVersion(Long version) { this.version = version; }
}
