package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.csu.mypetstore.model.dto.CustomerListItem;
import org.csu.mypetstore.model.query.CustomerListQuery;

public interface CustomerQueryService {
    IPage<CustomerListItem> findCustomers(CustomerListQuery query);
}
