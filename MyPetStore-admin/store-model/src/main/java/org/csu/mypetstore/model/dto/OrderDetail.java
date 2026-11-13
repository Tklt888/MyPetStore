package org.csu.mypetstore.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetail(Long id, String orderNo, Long userId, String username, String displayName,
                          String status, BigDecimal totalAmount, String receiverName,
                          String receiverPhone, String receiverAddress, LocalDateTime createdAt,
                          LocalDateTime updatedAt, String paymentMethod, String paymentNo,
                          LocalDateTime paidAt, LocalDateTime cancelledAt, LocalDateTime shippedAt,
                          LocalDateTime completedAt, List<OrderItemSnapshot> items) {
    public OrderDetail withItems(List<OrderItemSnapshot> orderItems) {
        return new OrderDetail(id, orderNo, userId, username, displayName, status, totalAmount,
                receiverName, receiverPhone, receiverAddress, createdAt, updatedAt, paymentMethod,
                paymentNo, paidAt, cancelledAt, shippedAt, completedAt, List.copyOf(orderItems));
    }
}
