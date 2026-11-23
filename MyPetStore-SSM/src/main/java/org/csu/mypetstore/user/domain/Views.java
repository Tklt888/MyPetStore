package org.csu.mypetstore.user.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Views {
    private Views() {}
    public record CategoryView(Long id,String code,Long parentId,String name,Integer sortOrder) {}
    public record ProductCard(Long id,String sku,String name,String summary,String coverImage,BigDecimal price,Integer stock,Long categoryId,String categoryName,Long parentId,String parentName,LocalDateTime createdAt) {}
    public record ProductDetailRow(Long id,String sku,String name,String summary,String description,String coverImage,BigDecimal price,Integer stock,Long categoryId,String categoryName,Long parentId,String parentName) {}
    public record ProductDetail(Long id,String sku,String name,String summary,String description,String coverImage,BigDecimal price,Integer stock,Long categoryId,String categoryName,Long parentId,String parentName,List<ImageView> images) {}
    public record ImageView(Long id,String imagePath,String altText) {}
    public record CartLine(Long id,Long productId,Long version,String sku,String name,String coverImage,BigDecimal price,Integer stock,String status,Integer quantity) { public BigDecimal subtotal(){return price.multiply(BigDecimal.valueOf(quantity));} public boolean available(){return "ON_SALE".equals(status)&&stock>=quantity;} }
    public record OrderLine(Long id,Long productId,String skuSnapshot,String productNameSnapshot,String coverImageSnapshot,BigDecimal unitPrice,Integer quantity,BigDecimal lineAmount) {}
    public record OrderListLine(Long orderId,String productNameSnapshot,String coverImageSnapshot,Integer quantity) {}
    public record OrderThumb(String name,String coverImage) {}
    public record OrderCard(Long id,String orderNo,String status,BigDecimal totalAmount,LocalDateTime createdAt,List<OrderThumb> thumbnails,Integer itemCount,Integer hiddenItemCount,String itemSummary) {
        public String statusLabel(){return switch(status){case "PENDING_PAYMENT"->"待支付";case "PAID"->"待发货";case "SHIPPED"->"已发货";case "COMPLETED"->"已完成";default->"已取消";};}
        public String statusClass(){return switch(status){case "PENDING_PAYMENT"->"pending";case "PAID"->"paid";case "SHIPPED"->"shipped";case "COMPLETED"->"completed";default->"cancelled";};}
        public boolean cancellable(){return "PENDING_PAYMENT".equals(status);}
        public boolean hasHiddenItems(){return hiddenItemCount>0;}
        public String displayOrderNo(){return orderNo!=null&&orderNo.startsWith("DEMO-")?"MP-"+orderNo.substring(5):orderNo;}
    }
    public record CheckoutToken(List<Long> itemIds,List<Long> versions,BigDecimal total) implements java.io.Serializable {}
}
