package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.csu.mypetstore.model.dto.OrderDetail;
import org.csu.mypetstore.model.dto.OrderListItem;
import org.csu.mypetstore.model.query.OrderListQuery;

public interface OrderQueryService {
    IPage<OrderListItem> findOrders(OrderListQuery query);
    OrderDetail getOrder(long id);
}
