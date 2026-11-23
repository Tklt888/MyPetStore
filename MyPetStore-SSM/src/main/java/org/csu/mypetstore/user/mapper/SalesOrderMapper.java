package org.csu.mypetstore.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;
import org.csu.mypetstore.user.domain.SalesOrder;

public interface SalesOrderMapper extends BaseMapper<SalesOrder> {
    @Select("""
      <script>select * from sales_order where user_id=#{userId} and archived_at is null
      <if test='status!=null and status!=""'>and status=#{status}</if>
      <if test='orderNo!=null and orderNo!=""'>and order_no like concat('%',#{orderNo},'%') escape '='</if>
      order by created_at desc,id desc</script>
      """) IPage<SalesOrder> owned(Page<SalesOrder> page,@Param("userId")long userId,@Param("status")String status,@Param("orderNo")String orderNo);
    @Select("select * from sales_order where id=#{id} and user_id=#{userId}") SalesOrder findOwned(@Param("id")long id,@Param("userId")long userId);
    @Update("update sales_order set status='PAID',payment_method='MOCK',payment_no=#{paymentNo},paid_at=now() where id=#{id} and user_id=#{userId} and status='PENDING_PAYMENT'") int pay(@Param("id")long id,@Param("userId")long userId,@Param("paymentNo")String paymentNo);
    @Update("update sales_order set status='CANCELLED',cancelled_at=now() where id=#{id} and user_id=#{userId} and status='PENDING_PAYMENT'") int cancel(@Param("id")long id,@Param("userId")long userId);
    @Update("update sales_order set status='COMPLETED',completed_at=now() where id=#{id} and user_id=#{userId} and status='SHIPPED'") int complete(@Param("id")long id,@Param("userId")long userId);
}
