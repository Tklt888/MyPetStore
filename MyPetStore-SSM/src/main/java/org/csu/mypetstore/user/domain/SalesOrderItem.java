package org.csu.mypetstore.user.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;

@TableName("sales_order_item")
public class SalesOrderItem {
    @TableId(type=IdType.AUTO) private Long id; private Long orderId; private Long productId; private String skuSnapshot; private String productNameSnapshot; private String coverImageSnapshot; private BigDecimal unitPrice; private Integer quantity; private BigDecimal lineAmount;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getOrderId(){return orderId;} public void setOrderId(Long v){orderId=v;} public Long getProductId(){return productId;} public void setProductId(Long v){productId=v;} public String getSkuSnapshot(){return skuSnapshot;} public void setSkuSnapshot(String v){skuSnapshot=v;} public String getProductNameSnapshot(){return productNameSnapshot;} public void setProductNameSnapshot(String v){productNameSnapshot=v;} public String getCoverImageSnapshot(){return coverImageSnapshot;} public void setCoverImageSnapshot(String v){coverImageSnapshot=v;} public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal v){unitPrice=v;} public Integer getQuantity(){return quantity;} public void setQuantity(Integer v){quantity=v;} public BigDecimal getLineAmount(){return lineAmount;} public void setLineAmount(BigDecimal v){lineAmount=v;}
}
