package org.csu.mypetstore.admin;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Select;
import org.csu.mypetstore.dao.CategoryMapper;
import org.csu.mypetstore.model.Category;
import org.csu.mypetstore.model.query.CategoryListQuery;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryMapperContractTest {

    @Test
    void categoryMapsOnlyWritableColumns() {
        TableName tableName = Category.class.getAnnotation(TableName.class);

        assertEquals("category", tableName.value());
        assertFalse(Arrays.stream(Category.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("parentKey")));
    }

    @Test
    void mapperExposesPagedTreeQueriesAndReferenceCounts() throws NoSuchMethodException {
        Method topLevel = CategoryMapper.class.getMethod("selectTopLevelPage", Page.class);
        Method children = CategoryMapper.class.getMethod("selectChildrenByParentIds", List.class);
        Method childCount = CategoryMapper.class.getMethod("countChildren", Long.class);
        Method productCount = CategoryMapper.class.getMethod("countProductReferences", Long.class);

        assertEquals(IPage.class, topLevel.getReturnType());
        assertEquals(List.class, children.getReturnType());
        assertEquals(long.class, childCount.getReturnType());
        assertEquals(long.class, productCount.getReturnType());

        String topSql = sqlOf(topLevel);
        String childSql = sqlOf(children);
        assertTrue(topSql.contains("parent_id IS NULL"));
        assertTrue(topSql.contains("ORDER BY sort_order ASC, id ASC"));
        assertTrue(childSql.contains("parent_id IN"));
        assertTrue(childSql.contains("ORDER BY sort_order ASC, id ASC"));
        assertTrue(sqlOf(childCount).contains("FROM category"));
        assertTrue(sqlOf(productCount).contains("FROM product"));
    }

    @Test
    void categoryListUsesTenTopLevelCategoriesPerPage() {
        CategoryListQuery query = new CategoryListQuery();

        assertEquals(1L, query.getPage());
        assertEquals(10L, CategoryListQuery.PAGE_SIZE);

        query.setPage(-2L);
        assertEquals(1L, query.getPage());

        query.setPage(3L);
        assertEquals(3L, query.getPage());
    }

    private String sqlOf(Method method) {
        return String.join("\n", method.getAnnotation(Select.class).value());
    }
}
