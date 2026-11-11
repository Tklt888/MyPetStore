package org.csu.mypetstore.model.command;

import java.math.BigDecimal;

public record ProductUpdateCommand(Long categoryId, String name, String summary, String description,
                                   String coverImage, BigDecimal price, Integer sortOrder, Long version) {
}
