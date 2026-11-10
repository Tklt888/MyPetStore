package org.csu.mypetstore.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.admin.config.MybatisPlusConfig;
import org.csu.mypetstore.dao.ProductMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductMapperContractTest {

    @Test
    void mapperExposesPagedProductQuery() throws NoSuchMethodException {
        Method method = ProductMapper.class.getMethod(
                "selectProductPage", Page.class, String.class, String.class);

        assertEquals(IPage.class, method.getReturnType());
    }

    @Test
    void configCreatesPaginationInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusConfig().mybatisPlusInterceptor();

        assertNotNull(interceptor);
        assertEquals(1, interceptor.getInterceptors().size());
    }
}
