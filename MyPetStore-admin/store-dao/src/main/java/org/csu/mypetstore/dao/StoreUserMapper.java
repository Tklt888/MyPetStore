package org.csu.mypetstore.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.csu.mypetstore.model.StoreUser;
import org.csu.mypetstore.model.dto.CustomerListItem;

@Mapper
public interface StoreUserMapper extends BaseMapper<StoreUser> {

    @Select("""
            <script>
            SELECT id, username, display_name AS displayName, email, phone,
                   status, created_at AS createdAt
              FROM store_user
             <where>
               role = 'CUSTOMER'
               <if test="keywordPattern != null">AND username LIKE CONCAT('%', #{keywordPattern}, '%') ESCAPE '\\'</if>
               <if test="status != null">AND status = #{status}</if>
             </where>
             ORDER BY id ASC
            </script>
            """)
    IPage<CustomerListItem> selectCustomerPage(Page<CustomerListItem> page,
                                                @Param("keywordPattern") String keywordPattern,
                                                @Param("status") String status);

    @Select("""
            SELECT id, username, password_hash, display_name, role, status
              FROM store_user
             WHERE username = #{username}
            """)
    StoreUser selectByUsername(@Param("username") String username);

    @Select("""
            SELECT id, role, status
              FROM store_user
             WHERE id = #{userId}
            """)
    StoreUser selectRoleStatusById(@Param("userId") Long userId);

    @Update("""
            UPDATE store_user
               SET status = #{targetStatus}
             WHERE id = #{id}
               AND role = 'CUSTOMER'
               AND status = #{oldStatus}
            """)
    int updateCustomerStatus(@Param("id") Long userId,
                             @Param("oldStatus") String oldStatus,
                             @Param("targetStatus") String targetStatus);

    @Select("""
            SELECT id, role, status
              FROM store_user
             WHERE id = #{userId}
             FOR UPDATE
            """)
    StoreUser selectRoleStatusForUpdate(@Param("userId") Long userId);
}
