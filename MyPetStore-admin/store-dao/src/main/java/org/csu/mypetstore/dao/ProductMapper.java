package org.csu.mypetstore.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.csu.mypetstore.model.Product;
import org.csu.mypetstore.model.dto.ProductCategoryOption;
import org.csu.mypetstore.model.dto.ProductEditView;
import org.csu.mypetstore.model.dto.ProductListItem;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("""
            <script>
            SELECT p.id, p.sku, p.name, p.category_id,
                   child.name AS category_name, parent.id AS top_category_id,
                   parent.name AS top_category_name, p.price, p.stock, p.status,
                   p.sort_order, p.version
              FROM product p
              JOIN category child ON child.id = p.category_id
              LEFT JOIN category parent ON parent.id = child.parent_id
             <where>
               <if test="keyword != null">AND (p.name LIKE CONCAT('%', #{keyword}, '%') OR p.sku LIKE CONCAT('%', #{keyword}, '%'))</if>
               <if test="status != null">AND p.status = #{status}</if>
               <if test="topCategoryId != null">AND parent.id = #{topCategoryId}</if>
               <if test="categoryId != null">AND p.category_id = #{categoryId}</if>
             </where>
             ORDER BY p.sort_order ASC, p.id ASC
            </script>
            """)
    IPage<ProductListItem> selectProductPage(Page<ProductListItem> page,
            @Param("keyword") String keyword, @Param("status") String status,
            @Param("topCategoryId") Long topCategoryId, @Param("categoryId") Long categoryId);

    @Select("""
            SELECT p.id, p.sku, p.category_id, p.name, p.summary, p.description,
                   p.cover_image, p.price, p.stock, p.status, p.sort_order, p.version
              FROM product p WHERE p.id = #{id}
            """)
    ProductEditView selectDetailById(@Param("id") Long id);

    @Select("""
            SELECT child.id, child.parent_id, parent.name AS parent_name, child.name
              FROM category child JOIN category parent ON parent.id = child.parent_id
             WHERE child.parent_id IS NOT NULL
             ORDER BY parent.sort_order ASC, parent.id ASC, child.sort_order ASC, child.id ASC
            """)
    List<ProductCategoryOption> selectCategoryOptions();

    @Select("SELECT * FROM product WHERE sku = #{sku}")
    Product selectBySku(@Param("sku") String sku);

    @Update("""
            UPDATE product SET category_id = #{categoryId}, name = #{name}, summary = #{summary},
                   description = #{description}, cover_image = #{coverImage}, price = #{price},
                   sort_order = #{sortOrder}, version = version + 1
             WHERE id = #{id} AND version = #{version}
            """)
    int updateDetails(@Param("id") Long id, @Param("categoryId") Long categoryId,
            @Param("name") String name, @Param("summary") String summary,
            @Param("description") String description, @Param("coverImage") String coverImage,
            @Param("price") BigDecimal price, @Param("sortOrder") Integer sortOrder,
            @Param("version") Long version);

    @Update("""
            UPDATE product SET status = #{status}, version = version + 1
             WHERE id = #{id} AND version = #{version}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("version") Long version);

    @Update("""
            UPDATE product SET stock = stock + #{quantity}, version = version + 1
             WHERE id = #{id} AND stock <= 2147483647 - #{quantity}
            """)
    int incrementStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
