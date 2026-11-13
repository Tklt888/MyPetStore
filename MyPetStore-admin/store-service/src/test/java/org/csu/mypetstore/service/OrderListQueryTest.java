package org.csu.mypetstore.service;

import org.csu.mypetstore.model.query.OrderListQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderListQueryTest {

    @Test
    void defaultsAndTrimsOrderNumber() {
        OrderListQuery query = query("  ORD-2026-001  ", null, null, null);
        query.normalize();

        assertEquals("ORD-2026-001", query.normalizedOrderNo());
        assertNull(query.normalizedStatus());
        assertEquals(1L, query.normalizedPage());
        assertEquals(10L, query.normalizedPageSize());
    }

    @Test
    void allAndBlankMeanEveryStatus() {
        assertNull(normalizedStatus("ALL"));
        assertNull(normalizedStatus("  "));
    }

    @Test
    void acceptsEveryDefinedOrderStatus() {
        for (String status : List.of("PENDING_PAYMENT", "PAID", "SHIPPED", "COMPLETED", "CANCELLED")) {
            assertEquals(status, normalizedStatus(status));
        }
    }

    @Test
    void nonPositivePageUsesFirstPage() {
        OrderListQuery zero = query(null, null, 0L, 10L);
        OrderListQuery negative = query(null, null, -9L, 10L);
        zero.normalize();
        negative.normalize();

        assertEquals(1L, zero.normalizedPage());
        assertEquals(1L, negative.normalizedPage());
    }

    @Test
    void rejectsUnknownStatusLongOrderNumberAndInvalidPageSize() {
        assertThrows(IllegalArgumentException.class, () -> query(null, "UNKNOWN", 1L, 10L).normalize());
        assertThrows(IllegalArgumentException.class, () -> query("X".repeat(41), null, 1L, 10L).normalize());
        assertThrows(IllegalArgumentException.class, () -> query(null, null, 1L, 0L).normalize());
        assertThrows(IllegalArgumentException.class, () -> query(null, null, 1L, 51L).normalize());
    }

    @Test
    void acceptsPageSizeBoundaries() {
        OrderListQuery one = query(null, null, 1L, 1L);
        OrderListQuery fifty = query(null, null, 1L, 50L);
        one.normalize();
        fifty.normalize();

        assertEquals(1L, one.normalizedPageSize());
        assertEquals(50L, fifty.normalizedPageSize());
    }

    private String normalizedStatus(String status) {
        OrderListQuery query = query(null, status, 1L, 10L);
        query.normalize();
        return query.normalizedStatus();
    }

    private OrderListQuery query(String orderNo, String status, Long page, Long pageSize) {
        OrderListQuery query = new OrderListQuery();
        query.setOrderNo(orderNo);
        query.setStatus(status);
        query.setPage(page);
        query.setPageSize(pageSize);
        return query;
    }
}
