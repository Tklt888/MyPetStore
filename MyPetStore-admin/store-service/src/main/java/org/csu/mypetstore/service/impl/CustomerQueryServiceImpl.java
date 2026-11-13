package org.csu.mypetstore.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.dao.StoreUserMapper;
import org.csu.mypetstore.model.dto.CustomerListItem;
import org.csu.mypetstore.model.query.CustomerListQuery;
import org.csu.mypetstore.service.CustomerQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerQueryServiceImpl implements CustomerQueryService {
    private final StoreUserMapper mapper;
    public CustomerQueryServiceImpl(StoreUserMapper mapper) { this.mapper = mapper; }

    @Override
    public IPage<CustomerListItem> findCustomers(CustomerListQuery query) {
        query.normalize();
        return mapper.selectCustomerPage(new Page<>(query.normalizedPage(), query.normalizedPageSize()),
                query.normalizedKeywordPattern(), query.normalizedStatus());
    }
}
