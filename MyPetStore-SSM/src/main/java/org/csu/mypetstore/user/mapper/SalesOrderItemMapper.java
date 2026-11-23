package org.csu.mypetstore.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.csu.mypetstore.user.domain.SalesOrderItem;
import org.csu.mypetstore.user.domain.Views.OrderLine;
import org.csu.mypetstore.user.domain.Views.OrderListLine;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface SalesOrderItemMapper extends BaseMapper<SalesOrderItem> {
    @Select("""
      select i.id,i.product_id,i.sku_snapshot,i.product_name_snapshot,
      coalesce(nullif(p.cover_image,''),i.cover_image_snapshot) as cover_image_snapshot,
      i.unit_price,i.quantity,i.line_amount
      from sales_order_item i left join product p on p.id=i.product_id
      where i.order_id=#{orderId} order by i.id
      """) List<OrderLine> lines(long orderId);
    @Select("""
      <script>select i.order_id,i.product_name_snapshot,
      coalesce(nullif(p.cover_image,''),i.cover_image_snapshot) as cover_image_snapshot,i.quantity
      from sales_order_item i left join product p on p.id=i.product_id where i.order_id in
      <foreach collection='orderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
      order by i.order_id,i.id</script>
      """) List<OrderListLine> listLines(@Param("orderIds")List<Long> orderIds);
}
