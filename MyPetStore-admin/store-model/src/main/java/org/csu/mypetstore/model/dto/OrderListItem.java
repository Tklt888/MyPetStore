package org.csu.mypetstore.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderListItem(Long id, String orderNo, Long userId, String username,
                            String displayName, BigDecimal totalAmount, String status,
                            LocalDateTime createdAt, LocalDateTime paidAt, LocalDateTime shippedAt) {}
