package org.csu.mypetstore.service.impl;

import org.csu.mypetstore.dao.SalesOrderMapper;
import org.csu.mypetstore.model.SalesOrder;
import org.csu.mypetstore.service.OrderFulfillmentService;
import org.csu.mypetstore.service.OrderNotFoundException;
import org.csu.mypetstore.service.OrderStateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class OrderFulfillmentServiceImpl implements OrderFulfillmentService {
    private static final Set<String> ALREADY_PROCESSED = Set.of("SHIPPED", "COMPLETED");
    private final SalesOrderMapper mapper;

    public OrderFulfillmentServiceImpl(SalesOrderMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void ship(long orderId) {
        if (mapper.markShippedIfPaid(orderId) == 1) return;

        SalesOrder current = mapper.selectById(orderId);
        if (current == null) throw new OrderNotFoundException(orderId);
        if (ALREADY_PROCESSED.contains(current.getStatus())) {
            throw new OrderStateException("订单已经处理，不能重复发货");
        }
        throw new OrderStateException("当前订单状态不能发货");
    }
}
