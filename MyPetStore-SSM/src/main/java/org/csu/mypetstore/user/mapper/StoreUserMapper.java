package org.csu.mypetstore.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.csu.mypetstore.user.domain.StoreUser;

public interface StoreUserMapper extends BaseMapper<StoreUser> {
    @Select("select * from store_user where lower(username)=lower(#{username}) limit 1") StoreUser findByUsername(String username);
}
