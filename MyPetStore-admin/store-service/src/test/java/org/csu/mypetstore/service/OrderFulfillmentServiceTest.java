package org.csu.mypetstore.service;

import org.csu.mypetstore.dao.SalesOrderMapper;
import org.csu.mypetstore.model.SalesOrder;
import org.csu.mypetstore.service.impl.OrderFulfillmentServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderFulfillmentServiceTest {

    @Test
    void paidOrderShipsWithSingleConditionalUpdate() {
        SalesOrderMapper mapper = mock(SalesOrderMapper.class);
        when(mapper.markShippedIfPaid(8L)).thenReturn(1);

        new OrderFulfillmentServiceImpl(mapper).ship(8L);

        verify(mapper).markShippedIfPaid(8L);
    }

    @Test
    void missingOrderAfterFailedUpdateUsesNotFound() {
        SalesOrderMapper mapper = mock(SalesOrderMapper.class);
        when(mapper.markShippedIfPaid(404L)).thenReturn(0);
        when(mapper.selectById(404L)).thenReturn(null);

        assertThrows(OrderNotFoundException.class,
                () -> new OrderFulfillmentServiceImpl(mapper).ship(404L));
    }

    @Test
    void repeatedOrCompletedShipmentReportsAlreadyProcessed() {
        for (String status : new String[]{"SHIPPED", "COMPLETED"}) {
            SalesOrderMapper mapper = mock(SalesOrderMapper.class);
            when(mapper.markShippedIfPaid(8L)).thenReturn(0);
            when(mapper.selectById(8L)).thenReturn(order(8L, status));

            OrderStateException error = assertThrows(OrderStateException.class,
                    () -> new OrderFulfillmentServiceImpl(mapper).ship(8L));
            assertEquals("订单已经处理，不能重复发货", error.getMessage());
        }
    }

    @Test
    void unpaidOrCancelledOrderCannotShip() {
        for (String status : new String[]{"PENDING_PAYMENT", "CANCELLED"}) {
            SalesOrderMapper mapper = mock(SalesOrderMapper.class);
            when(mapper.markShippedIfPaid(8L)).thenReturn(0);
            when(mapper.selectById(8L)).thenReturn(order(8L, status));

            OrderStateException error = assertThrows(OrderStateException.class,
                    () -> new OrderFulfillmentServiceImpl(mapper).ship(8L));
            assertEquals("当前订单状态不能发货", error.getMessage());
        }
    }

    private SalesOrder order(Long id, String status) {
        SalesOrder order = new SalesOrder();
        order.setId(id); order.setStatus(status);
        return order;
    }
}
