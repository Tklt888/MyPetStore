package org.csu.mypetstore.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
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

    @Test
    void statusUpdateChangesOnlyStatusForExpectedCustomerState() throws Exception {
        Method method = StoreUserMapper.class.getMethod(
                "updateCustomerStatus", Long.class, String.class, String.class);
        String sql = String.join("\n", method.getAnnotation(Update.class).value());
        String set = sql.substring(sql.indexOf("SET"), sql.indexOf("WHERE"));
        assertTrue(set.contains("status = #{targetStatus}"));
        assertTrue(sql.contains("id = #{id}"));
        assertTrue(sql.contains("role = 'CUSTOMER'"));
        assertTrue(sql.contains("status = #{oldStatus}"));
        assertFalse(set.contains("password"));
        assertFalse(set.contains("role"));
        assertFalse(set.contains("username"));
    }

    @Test
    void failedUpdateDiagnosisUsesCurrentLockingRead() throws Exception {
        Method method = StoreUserMapper.class.getMethod("selectRoleStatusForUpdate", Long.class);
        String sql = String.join("\n", method.getAnnotation(Select.class).value());
        assertTrue(sql.contains("SELECT id, role, status"));
        assertTrue(sql.contains("FOR UPDATE"));
        assertFalse(sql.toLowerCase().contains("password"));
    }
}
