package org.csu.mypetstore.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.csu.mypetstore.user.domain.CartItem;
import org.csu.mypetstore.user.domain.Views.CartLine;
import java.util.List;

public interface CartItemMapper extends BaseMapper<CartItem> {
    @Select("select * from cart_item where user_id=#{userId} and product_id=#{productId} for update") CartItem lockExisting(@Param("userId")long userId,@Param("productId")long productId);
    @Select("select c.id,c.product_id,c.version,p.sku,p.name,case when p.sku='REP-HEAT-26' then 'images/catalog/REP-HEAT-26.jpg' else p.cover_image end cover_image,p.price,p.stock,p.status,c.quantity from cart_item c join product p on p.id=c.product_id where c.user_id=#{userId} order by c.created_at,c.id") List<CartLine> lines(long userId);
    @Select("select coalesce(sum(quantity),0) from cart_item where user_id=#{userId}") int count(long userId);
    @Select("select count(*) from cart_item where user_id=#{userId}") int kindCount(long userId);
    @Update("update cart_item set quantity=#{quantity},version=version+1 where id=#{id} and user_id=#{userId} and version=#{version}") int change(@Param("id")long id,@Param("userId")long userId,@Param("quantity")int quantity,@Param("version")long version);
    @Delete("delete from cart_item where id=#{id} and user_id=#{userId} and version=#{version}") int removeOwned(@Param("id")long id,@Param("userId")long userId,@Param("version")long version);
    @Delete("<script>delete from cart_item where user_id=#{userId} and id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>") int deleteChecked(@Param("userId")long userId,@Param("ids")List<Long> ids);
}
