package org.csu.mypetstore.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.csu.mypetstore.model.StoreUser;

@Mapper
public interface StoreUserMapper extends BaseMapper<StoreUser> {

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
}
