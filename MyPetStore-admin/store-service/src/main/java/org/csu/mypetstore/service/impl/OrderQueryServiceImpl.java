package org.csu.mypetstore.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.dao.SalesOrderMapper;
import org.csu.mypetstore.model.dto.OrderDetail;
import org.csu.mypetstore.model.dto.OrderListItem;
import org.csu.mypetstore.model.query.OrderListQuery;
import org.csu.mypetstore.service.OrderNotFoundException;
import org.csu.mypetstore.service.OrderQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderQueryServiceImpl implements OrderQueryService {
    private final SalesOrderMapper mapper;

    public OrderQueryServiceImpl(SalesOrderMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public IPage<OrderListItem> findOrders(OrderListQuery query) {
        query.normalize();
        return mapper.selectOrderPage(new Page<>(query.normalizedPage(), query.normalizedPageSize()),
                query.normalizedOrderNo(), query.normalizedStatus());
    }

    @Override
    public OrderDetail getOrder(long id) {
        OrderDetail detail = mapper.selectOrderDetail(id);
        if (detail == null) throw new OrderNotFoundException(id);
        return detail.withItems(mapper.selectOrderItems(id));
    }
}
