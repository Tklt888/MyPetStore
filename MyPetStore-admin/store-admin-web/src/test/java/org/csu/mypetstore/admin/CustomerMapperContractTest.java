package org.csu.mypetstore.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Select;
import org.csu.mypetstore.dao.StoreUserMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerMapperContractTest {
    @Test
    void customerPageIsRoleBoundExplicitAndNewestPasswordNeverSelected() throws Exception {
        Method method = StoreUserMapper.class.getMethod(
                "selectCustomerPage", Page.class, String.class, String.class);
        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertTrue(sql.contains("role = 'CUSTOMER'"));
        assertTrue(sql.contains("username LIKE"));
        assertTrue(sql.contains("ESCAPE"));
        assertTrue(sql.contains("status = #{status}"));
        assertTrue(sql.contains("ORDER BY id ASC"));
        assertFalse(sql.toLowerCase().contains("password_hash"));
        assertFalse(sql.contains("SELECT *"));
    }
}
