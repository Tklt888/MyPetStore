package org.csu.mypetstore.service.impl;

import org.csu.mypetstore.dao.ProductMapper;
import org.csu.mypetstore.dao.SalesOrderMapper;
import org.csu.mypetstore.dao.StoreUserMapper;
import org.csu.mypetstore.model.dto.DashboardSnapshot;
import org.csu.mypetstore.service.DashboardService;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {
    private static final String PENDING_SHIPMENT = "PAID";
    private static final int LOW_STOCK_THRESHOLD = 3;
    private static final int LIST_LIMIT = 5;

    private final SalesOrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final StoreUserMapper userMapper;

    public DashboardServiceImpl(SalesOrderMapper orderMapper, ProductMapper productMapper, StoreUserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    @Override
    public DashboardSnapshot load() {
        return new DashboardSnapshot(
                pendingShipmentCount(),
                productMapper.countWithStockAtMost(LOW_STOCK_THRESHOLD),
                productMapper.countByStatus("ON_SALE"),
                userMapper.countCustomers(),
                orderMapper.selectRecentByStatus(PENDING_SHIPMENT, LIST_LIMIT),
                productMapper.selectLowStockItems(LOW_STOCK_THRESHOLD, LIST_LIMIT));
    }

    @Override
    public long pendingShipmentCount() {
        return orderMapper.countByStatus(PENDING_SHIPMENT);
    }
}
