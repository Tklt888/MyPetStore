package org.csu.mypetstore.user.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("cart_item")
public class CartItem {
    @TableId(type=IdType.AUTO) private Long id; private Long userId; private Long productId; private Integer quantity; private Long version; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public Long getProductId(){return productId;} public void setProductId(Long v){productId=v;} public Integer getQuantity(){return quantity;} public void setQuantity(Integer v){quantity=v;} public Long getVersion(){return version;} public void setVersion(Long v){version=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
