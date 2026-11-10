package org.csu.mypetstore.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.csu.mypetstore.model.Product;
import org.csu.mypetstore.model.dto.ProductListItem;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("""
            <script>
            SELECT p.id,
                   p.sku,
                   p.name,
                   c.name AS category_name,
                   p.price,
                   p.stock,
                   p.status
              FROM product p
              JOIN category c ON c.id = p.category_id
             <where>
               <if test="keyword != null">
                 AND (p.name LIKE CONCAT('%', #{keyword}, '%')
                      OR p.sku LIKE CONCAT('%', #{keyword}, '%'))
               </if>
               <if test="status != null">
                 AND p.status = #{status}
               </if>
             </where>
             ORDER BY p.sort_order ASC, p.id ASC
            </script>
            """)
    IPage<ProductListItem> selectProductPage(
            Page<ProductListItem> page,
            @Param("keyword") String keyword,
            @Param("status") String status);
}
