package org.csu.mypetstore.admin.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ProductRestockForm {
    @NotNull @Min(1) private Integer quantity;
    public Integer getQuantity() { return quantity; } public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
