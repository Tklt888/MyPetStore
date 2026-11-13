package org.csu.mypetstore.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.csu.mypetstore.model.SalesOrder;
import org.csu.mypetstore.model.dto.OrderDetail;
import org.csu.mypetstore.model.dto.OrderItemSnapshot;
import org.csu.mypetstore.model.dto.OrderListItem;

import java.util.List;

@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    @Select("""
            <script>
            SELECT o.id, o.order_no AS orderNo, o.user_id AS userId,
                   u.username, u.display_name AS displayName,
                   o.total_amount AS totalAmount, o.status,
                   o.created_at AS createdAt, o.paid_at AS paidAt, o.shipped_at AS shippedAt
              FROM sales_order o JOIN store_user u ON u.id = o.user_id
             <where>
               <if test="orderNo != null">AND o.order_no = #{orderNo}</if>
               <if test="status != null">AND o.status = #{status}</if>
             </where>
             ORDER BY o.created_at DESC, o.id DESC
            </script>
            """)
    IPage<OrderListItem> selectOrderPage(Page<OrderListItem> page,
                                         @Param("orderNo") String orderNo,
                                         @Param("status") String status);

    @Select("""
            SELECT o.id, o.order_no AS orderNo, o.user_id AS userId,
                   u.username, u.display_name AS displayName, o.status,
                   o.total_amount AS totalAmount, o.receiver_name AS receiverName,
                   o.receiver_phone AS receiverPhone, o.receiver_address AS receiverAddress,
                   o.created_at AS createdAt, o.updated_at AS updatedAt,
                   o.payment_method AS paymentMethod, o.payment_no AS paymentNo,
                   o.paid_at AS paidAt, o.cancelled_at AS cancelledAt,
                   o.shipped_at AS shippedAt, o.completed_at AS completedAt
              FROM sales_order o JOIN store_user u ON u.id = o.user_id
             WHERE o.id = #{id}
            """)
    OrderDetail selectOrderDetail(@Param("id") Long id);

    @Select("""
            SELECT id, product_id AS productId, sku_snapshot AS skuSnapshot,
                   product_name_snapshot AS productNameSnapshot,
                   cover_image_snapshot AS coverImageSnapshot,
                   unit_price AS unitPrice, quantity, line_amount AS lineAmount
              FROM sales_order_item
             WHERE order_id = #{orderId}
             ORDER BY id ASC
            """)
    List<OrderItemSnapshot> selectOrderItems(@Param("orderId") Long orderId);
}
