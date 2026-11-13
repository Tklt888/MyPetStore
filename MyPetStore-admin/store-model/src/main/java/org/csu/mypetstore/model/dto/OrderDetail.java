package org.csu.mypetstore.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDetail {
    private Long id; private String orderNo; private Long userId; private String username;
    private String displayName; private String status; private BigDecimal totalAmount;
    private String receiverName; private String receiverPhone; private String receiverAddress;
    private LocalDateTime createdAt; private LocalDateTime updatedAt; private String paymentMethod;
    private String paymentNo; private LocalDateTime paidAt; private LocalDateTime cancelledAt;
    private LocalDateTime shippedAt; private LocalDateTime completedAt;
    private List<OrderItemSnapshot> items = List.of();

    public OrderDetail() {}

    public OrderDetail(Long id, String orderNo, Long userId, String username, String displayName,
                       String status, BigDecimal totalAmount, String receiverName, String receiverPhone,
                       String receiverAddress, LocalDateTime createdAt, LocalDateTime updatedAt,
                       String paymentMethod, String paymentNo, LocalDateTime paidAt,
                       LocalDateTime cancelledAt, LocalDateTime shippedAt, LocalDateTime completedAt,
                       List<OrderItemSnapshot> items) {
        this.id = id; this.orderNo = orderNo; this.userId = userId; this.username = username;
        this.displayName = displayName; this.status = status; this.totalAmount = totalAmount;
        this.receiverName = receiverName; this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress; this.createdAt = createdAt; this.updatedAt = updatedAt;
        this.paymentMethod = paymentMethod; this.paymentNo = paymentNo; this.paidAt = paidAt;
        this.cancelledAt = cancelledAt; this.shippedAt = shippedAt; this.completedAt = completedAt;
        setItems(items);
    }

    public OrderDetail withItems(List<OrderItemSnapshot> orderItems) { setItems(orderItems); return this; }
    public List<OrderItemSnapshot> items() { return items; }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; } public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; } public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; } public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getReceiverName() { return receiverName; } public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; } public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; } public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentNo() { return paymentNo; } public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
    public LocalDateTime getPaidAt() { return paidAt; } public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; } public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getShippedAt() { return shippedAt; } public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public List<OrderItemSnapshot> getItems() { return items; }
    public void setItems(List<OrderItemSnapshot> items) { this.items = items == null ? List.of() : List.copyOf(items); }
}
