package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.dao.StoreUserMapper;
import org.csu.mypetstore.model.dto.CustomerListItem;
import org.csu.mypetstore.model.query.CustomerListQuery;
import org.csu.mypetstore.service.impl.CustomerQueryServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerQueryServiceTest {
    @Test
    void passesOnlyNormalizedFiltersAndPageToMapper() {
        StoreUserMapper mapper = mock(StoreUserMapper.class);
        when(mapper.selectCustomerPage(any(), any(), any())).thenAnswer(call -> call.getArgument(0));
        CustomerListQuery query = new CustomerListQuery();
        query.setKeyword("  a_b%  "); query.setStatus("enabled"); query.setPage(3L); query.setPageSize(25L);

        new CustomerQueryServiceImpl(mapper).findCustomers(query);

        ArgumentCaptor<Page<CustomerListItem>> page = ArgumentCaptor.forClass(Page.class);
        verify(mapper).selectCustomerPage(page.capture(), eq("a\\_b\\%"), eq("ENABLED"));
        assertEquals(3L, page.getValue().getCurrent());
        assertEquals(25L, page.getValue().getSize());
    }
}
