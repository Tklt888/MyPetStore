package org.csu.mypetstore.service;

import org.csu.mypetstore.dao.ProductMapper;
import org.csu.mypetstore.dao.SalesOrderMapper;
import org.csu.mypetstore.dao.StoreUserMapper;
import org.csu.mypetstore.model.dto.DashboardSnapshot;
import org.csu.mypetstore.model.dto.DashboardStockItem;
import org.csu.mypetstore.model.dto.OrderListItem;
import org.csu.mypetstore.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    @Test
    void snapshotContainsRealCountsAndLimitedWorkQueues() {
        SalesOrderMapper orders = mock(SalesOrderMapper.class);
        ProductMapper products = mock(ProductMapper.class);
        StoreUserMapper users = mock(StoreUserMapper.class);
        OrderListItem order = new OrderListItem(7L, "ORD-7", 3L, "lin", "林同学",
                new BigDecimal("128.00"), "PAID", LocalDateTime.of(2026, 11, 17, 9, 30), null, null);
        DashboardStockItem stock = new DashboardStockItem(9L, "低库存商品", "/images/p9.jpg", 1, "ON_SALE");
        when(orders.countByStatus("PAID")).thenReturn(3L);
        when(products.countWithStockAtMost(3)).thenReturn(5L);
        when(products.countByStatus("ON_SALE")).thenReturn(42L);
        when(users.countCustomers()).thenReturn(18L);
        when(orders.selectRecentByStatus("PAID", 5)).thenReturn(List.of(order));
        when(products.selectLowStockItems(3, 5)).thenReturn(List.of(stock));

        DashboardSnapshot snapshot = new DashboardServiceImpl(orders, products, users).load();

        assertEquals(3L, snapshot.pendingShipmentCount());
        assertEquals(5L, snapshot.lowStockCount());
        assertEquals(42L, snapshot.onSaleProductCount());
        assertEquals(18L, snapshot.customerCount());
        assertEquals(List.of(order), snapshot.pendingOrders());
        assertEquals(List.of(stock), snapshot.lowStockProducts());
    }
}
