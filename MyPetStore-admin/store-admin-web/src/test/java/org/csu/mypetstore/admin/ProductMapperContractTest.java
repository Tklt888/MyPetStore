package org.csu.mypetstore.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.admin.config.MybatisPlusConfig;
import org.csu.mypetstore.dao.ProductMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductMapperContractTest {

    @Test
    void mapperExposesPagedProductQuery() throws NoSuchMethodException {
        Method method = ProductMapper.class.getMethod(
                "selectProductPage", Page.class, String.class, String.class, Long.class, Long.class);

        assertEquals(IPage.class, method.getReturnType());
        String sql = selectSqlOf(method);
        assertTrue(sql.contains("LEFT JOIN category parent"));
        assertTrue(sql.contains("parent.id = #{topCategoryId}"));
        assertTrue(sql.contains("p.category_id = #{categoryId}"));
        assertTrue(sql.contains("ORDER BY p.sort_order ASC, p.id ASC"));
    }

    @Test
    void mapperExposesMaintenanceQueries() throws NoSuchMethodException {
        Method detail = ProductMapper.class.getMethod("selectDetailById", Long.class);
        Method options = ProductMapper.class.getMethod("selectCategoryOptions");
        Method sku = ProductMapper.class.getMethod("selectBySku", String.class);

        assertTrue(selectSqlOf(detail).contains("p.version"));
        assertEquals(List.class, options.getReturnType());
        assertTrue(selectSqlOf(options).contains("child.parent_id IS NOT NULL"));
        assertTrue(selectSqlOf(sku).contains("WHERE sku = #{sku}"));
    }

    @Test
    void maintenanceUpdatesUseVersionWithoutOverwritingProtectedFields() throws NoSuchMethodException {
        Method details = ProductMapper.class.getMethod("updateDetails", Long.class, Long.class,
                String.class, String.class, String.class, String.class, BigDecimal.class, Integer.class, Long.class);
        Method status = ProductMapper.class.getMethod("updateStatus", Long.class, String.class, Long.class);
        Method stock = ProductMapper.class.getMethod("incrementStock", Long.class, Integer.class);

        String detailSql = updateSqlOf(details);
        String setClause = detailSql.substring(detailSql.indexOf("SET"), detailSql.indexOf("WHERE"));
        assertTrue(detailSql.contains("version = version + 1"));
        assertTrue(detailSql.contains("id = #{id} AND version = #{version}"));
        assertFalse(setClause.contains("stock"));
        assertFalse(setClause.contains("status"));
        assertFalse(setClause.contains("sku"));

        String statusSql = updateSqlOf(status);
        assertTrue(statusSql.contains("status = #{status}"));
        assertTrue(statusSql.contains("version = version + 1"));
        assertTrue(statusSql.contains("id = #{id} AND version = #{version}"));

        String stockSql = updateSqlOf(stock);
        assertTrue(stockSql.contains("stock = stock + #{quantity}"));
        assertTrue(stockSql.contains("version = version + 1"));
        assertTrue(stockSql.contains("stock <= 2147483647 - #{quantity}"));
        assertTrue(stockSql.contains("id = #{id}"));
        assertFalse(stockSql.contains("version = #{version}"));
    }

    @Test
    void configCreatesPaginationInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusConfig().mybatisPlusInterceptor();

        assertNotNull(interceptor);
        assertEquals(1, interceptor.getInterceptors().size());
    }

    private String selectSqlOf(Method method) {
        return String.join("\n", method.getAnnotation(Select.class).value());
    }

    private String updateSqlOf(Method method) {
        return String.join("\n", method.getAnnotation(Update.class).value());
    }
}
