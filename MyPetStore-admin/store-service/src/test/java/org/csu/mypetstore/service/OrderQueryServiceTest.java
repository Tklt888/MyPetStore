package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.dao.SalesOrderMapper;
import org.csu.mypetstore.model.dto.OrderDetail;
import org.csu.mypetstore.model.dto.OrderItemSnapshot;
import org.csu.mypetstore.model.dto.OrderListItem;
import org.csu.mypetstore.model.query.OrderListQuery;
import org.csu.mypetstore.service.impl.OrderQueryServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderQueryServiceTest {

    @Test
    void listNormalizesFiltersAndPageSize() {
        SalesOrderMapper mapper = mock(SalesOrderMapper.class);
        when(mapper.selectOrderPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        OrderQueryService service = new OrderQueryServiceImpl(mapper);
        OrderListQuery query = new OrderListQuery();
        query.setOrderNo("  ORD-1  "); query.setStatus("paid"); query.setPage(3L); query.setPageSize(25L);

        service.findOrders(query);

        ArgumentCaptor<Page<OrderListItem>> page = ArgumentCaptor.forClass(Page.class);
        verify(mapper).selectOrderPage(page.capture(), org.mockito.ArgumentMatchers.eq("ORD-1"),
                org.mockito.ArgumentMatchers.eq("PAID"));
        assertEquals(3L, page.getValue().getCurrent());
        assertEquals(25L, page.getValue().getSize());
    }

    @Test
    void detailCombinesStoredItemSnapshots() {
        SalesOrderMapper mapper = mock(SalesOrderMapper.class);
        OrderDetail detail = detail(8L, "PAID");
        OrderItemSnapshot item = new OrderItemSnapshot(1L, 7L, "OLD-SKU", "成交时名称",
                "images/old.png", new BigDecimal("12.50"), 2, new BigDecimal("25.00"));
        when(mapper.selectOrderDetail(8L)).thenReturn(detail);
        when(mapper.selectOrderItems(8L)).thenReturn(List.of(item));

        OrderDetail result = new OrderQueryServiceImpl(mapper).getOrder(8L);

        assertEquals(List.of(item), result.items());
        assertEquals("成交时名称", result.items().getFirst().productNameSnapshot());
    }

    @Test
    void missingDetailUsesNotFound() {
        SalesOrderMapper mapper = mock(SalesOrderMapper.class);
        when(mapper.selectOrderDetail(404L)).thenReturn(null);

        assertThrows(OrderNotFoundException.class,
                () -> new OrderQueryServiceImpl(mapper).getOrder(404L));
    }

    private OrderDetail detail(Long id, String status) {
        LocalDateTime now = LocalDateTime.of(2026, 11, 13, 10, 0);
        return new OrderDetail(id, "ORD-1", 2L, "customer", "顾客", status,
                new BigDecimal("25.00"), "张三", "13800000000", "长沙市",
                now, now, "MOCK", "PAY-1", now, null, null, null, List.of());
    }
}
