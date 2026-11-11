package org.csu.mypetstore.model.dto;

import java.math.BigDecimal;

public record ProductEditView(Long id, String sku, Long categoryId, String name, String summary,
                              String description, String coverImage, BigDecimal price, Integer stock,
                              String status, Integer sortOrder, Long version) {
}
