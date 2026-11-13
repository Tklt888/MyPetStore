package org.csu.mypetstore.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.csu.mypetstore.dao.SalesOrderMapper;
import org.csu.mypetstore.model.dto.OrderDetail;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderMapperContractTest {

    @Test
    void orderDetailSupportsMybatisPropertyMapping() throws Exception {
        OrderDetail.class.getDeclaredConstructor();
        OrderDetail.class.getMethod("setId", Long.class);
        OrderDetail.class.getMethod("setItems", java.util.List.class);
    }

    @Test
    void listUsesExactFiltersAndNewestFirst() throws Exception {
        Method method = SalesOrderMapper.class.getMethod(
                "selectOrderPage", Page.class, String.class, String.class);
        String sql = selectSql(method);

        assertTrue(sql.contains("o.order_no = #{orderNo}"));
        assertTrue(sql.contains("o.status = #{status}"));
        assertTrue(sql.contains("ORDER BY o.created_at DESC, o.id DESC"));
        assertTrue(sql.contains("JOIN store_user"));
    }

    @Test
    void detailAndItemsReadHistoricalSnapshots() throws Exception {
        String detail = selectSql(SalesOrderMapper.class.getMethod("selectOrderDetail", Long.class));
        String items = selectSql(SalesOrderMapper.class.getMethod("selectOrderItems", Long.class));

        assertTrue(detail.contains("JOIN store_user"));
        assertTrue(items.contains("FROM sales_order_item"));
        assertTrue(items.contains("ORDER BY id ASC"));
        assertFalse(items.contains("JOIN product"));
    }

    @Test
    void shippingOnlyChangesStatusAndFirstShippingTimeForPaidOrder() throws Exception {
        Method method = SalesOrderMapper.class.getMethod("markShippedIfPaid", Long.class);
        String sql = String.join("\n", method.getAnnotation(Update.class).value());
        String set = sql.substring(sql.indexOf("SET"), sql.indexOf("WHERE"));

        assertTrue(set.contains("status = 'SHIPPED'"));
        assertTrue(set.contains("shipped_at = CURRENT_TIMESTAMP"));
        assertTrue(sql.contains("id = #{id} AND status = 'PAID'"));
        assertFalse(set.contains("total_amount"));
        assertFalse(set.contains("payment"));
        assertFalse(set.contains("stock"));
    }

    private String selectSql(Method method) {
        return String.join("\n", method.getAnnotation(Select.class).value());
    }
}
