package org.csu.mypetstore.user.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("product")
public class Product {
    @TableId(type=IdType.AUTO) private Long id; private String sku; private Long categoryId; private String name; private String summary; private String description; private String coverImage; private BigDecimal price; private Integer stock; private String status; private Integer sortOrder; private Long version; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getSku(){return sku;} public void setSku(String v){sku=v;} public Long getCategoryId(){return categoryId;} public void setCategoryId(Long v){categoryId=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getSummary(){return summary;} public void setSummary(String v){summary=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getCoverImage(){return coverImage;} public void setCoverImage(String v){coverImage=v;} public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;} public Integer getStock(){return stock;} public void setStock(Integer v){stock=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;} public Long getVersion(){return version;} public void setVersion(Long v){version=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
