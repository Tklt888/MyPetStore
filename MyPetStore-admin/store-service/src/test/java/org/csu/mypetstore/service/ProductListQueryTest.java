package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.dao.ProductMapper;
import org.csu.mypetstore.model.dto.ProductListItem;
import org.csu.mypetstore.model.query.ProductListQuery;
import org.csu.mypetstore.service.impl.ProductQueryServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductListQueryTest {

    @Test
    void missingOrNonPositivePageUsesFirstPage() {
        ProductListQuery missing = new ProductListQuery();
        ProductListQuery negative = query(-8L, null, null);

        assertEquals(1L, missing.normalizedPage());
        assertEquals(1L, negative.normalizedPage());
    }

    @Test
    void keywordIsTrimmedAndBlankKeywordIsRemoved() {
        assertEquals("DOG-001", query(1L, "  DOG-001  ", null).normalizedKeyword());
        assertNull(query(1L, "   ", null).normalizedKeyword());
    }

    @Test
    void onlyKnownProductStatusesAreAccepted() {
        assertEquals("DRAFT", query(1L, null, "DRAFT").normalizedStatus());
        assertEquals("ON_SALE", query(1L, null, "ON_SALE").normalizedStatus());
        assertNull(query(1L, null, "REMOVED").normalizedStatus());
    }

    @Test
    void serviceAlwaysRequestsTenRows() {
        AtomicLong requestedSize = new AtomicLong();
        ProductMapper mapper = (ProductMapper) Proxy.newProxyInstance(
                ProductMapper.class.getClassLoader(),
                new Class<?>[]{ProductMapper.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("selectProductPage")) {
                        Page<?> page = (Page<?>) args[0];
                        requestedSize.set(page.getSize());
                        return new Page<ProductListItem>(page.getCurrent(), page.getSize());
                    }
                    return null;
                });
        ProductQueryService service = new ProductQueryServiceImpl(mapper);

        IPage<ProductListItem> result = service.findProducts(query(3L, null, null));

        assertEquals(10L, requestedSize.get());
        assertEquals(3L, result.getCurrent());
    }

    private ProductListQuery query(Long page, String keyword, String status) {
        ProductListQuery query = new ProductListQuery();
        query.setPage(page);
        query.setKeyword(keyword);
        query.setStatus(status);
        return query;
    }
}
