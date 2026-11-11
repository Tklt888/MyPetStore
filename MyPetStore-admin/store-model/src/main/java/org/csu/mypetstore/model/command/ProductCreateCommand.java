package org.csu.mypetstore.model.command;

import java.math.BigDecimal;

public record ProductCreateCommand(String sku, Long categoryId, String name, String summary,
                                   String description, String coverImage, BigDecimal price,
                                   Integer stock, Integer sortOrder) {
}
