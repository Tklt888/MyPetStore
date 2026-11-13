package org.csu.mypetstore.model.dto;

import java.math.BigDecimal;

public record OrderItemSnapshot(Long id, Long productId, String skuSnapshot,
                                String productNameSnapshot, String coverImageSnapshot,
                                BigDecimal unitPrice, Integer quantity, BigDecimal lineAmount) {}
