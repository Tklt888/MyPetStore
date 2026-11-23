package org.csu.mypetstore.user.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;
import org.csu.mypetstore.user.domain.Views.*;
import java.util.List;

public interface CatalogMapper {
    @Select("select id,code,parent_id,name,sort_order from category order by parent_id is not null,parent_id,sort_order,id") List<CategoryView> categories();
    @Select("select id,code,parent_id,name,sort_order from category where id=#{id}") CategoryView category(long id);
    @Select("""
      <script>select p.id,p.sku,p.name,p.summary,p.cover_image,p.price,p.stock,p.category_id,c.name category_name,c.parent_id,pc.name parent_name,p.created_at
      from product p join category c on c.id=p.category_id join category pc on pc.id=c.parent_id where p.status='ON_SALE'
      <if test='keyword!=null and keyword!=""'>and p.name like concat('%',#{keyword},'%') escape '='</if>
      <if test='categoryId!=null'>and (p.category_id=#{categoryId} or c.parent_id=#{categoryId})</if>
      <if test='type!=null and type!="ALL"'>and c.code like <choose><when test='type=="PETS"'>concat('%','_PETS')</when><otherwise>concat('%','_SUPPLIES')</otherwise></choose></if>
      order by <choose><when test='sort=="PRICE_ASC"'>p.price asc,p.id asc</when><when test='sort=="PRICE_DESC"'>p.price desc,p.id asc</when><when test='sort=="NEWEST"'>p.created_at desc,p.id desc</when><otherwise>case when p.cover_image like 'images/products/%' then 0 else 1 end,p.sort_order asc,p.id asc</otherwise></choose></script>
      """) IPage<ProductCard> search(Page<ProductCard> page,@Param("keyword")String keyword,@Param("categoryId")Long categoryId,@Param("type")String type,@Param("sort")String sort);
    @Select("""
      select p.id,p.sku,p.name,p.summary,p.cover_image,p.price,p.stock,p.category_id,c.name category_name,c.parent_id,pc.name parent_name,p.created_at
      from product p join category c on c.id=p.category_id join category pc on pc.id=c.parent_id where p.status='ON_SALE' order by p.created_at desc,p.id desc limit #{limit}
      """) List<ProductCard> newest(int limit);
    @Select("""
      select p.id,p.sku,p.name,p.summary,p.cover_image,p.price,p.stock,p.category_id,c.name category_name,c.parent_id,pc.name parent_name,p.created_at
      from product p join category c on c.id=p.category_id join category pc on pc.id=c.parent_id where p.status='ON_SALE' order by case when p.cover_image like 'images/products/%' then 0 else 1 end,p.sort_order,p.id limit #{limit}
      """) List<ProductCard> featured(int limit);
    @Select("""
      select p.id,p.sku,p.name,p.summary,p.description,p.cover_image,p.price,p.stock,p.category_id,c.name category_name,c.parent_id,pc.name parent_name
      from product p join category c on c.id=p.category_id join category pc on pc.id=c.parent_id where p.id=#{id} and p.status='ON_SALE'
      """) ProductDetailRow detail(long id);
    @Select("select id,image_path,alt_text from product_image where product_id=#{productId} order by sort_order,id") List<ImageView> images(long productId);
}
