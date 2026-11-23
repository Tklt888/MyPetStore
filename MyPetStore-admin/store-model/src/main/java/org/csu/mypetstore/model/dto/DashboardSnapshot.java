package org.csu.mypetstore.model.dto;

import java.util.List;

public record DashboardSnapshot(long pendingShipmentCount, long lowStockCount,
                                long onSaleProductCount, long customerCount,
                                List<OrderListItem> pendingOrders,
                                List<DashboardStockItem> lowStockProducts) {}
