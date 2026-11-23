package org.csu.mypetstore.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.csu.mypetstore.user.domain.Product;

public interface ProductMapper extends BaseMapper<Product> {
    @Select("select * from product where id=#{id} for update") Product lock(long id);
    @Update("update product set stock=stock-#{quantity},version=version+1 where id=#{id} and status='ON_SALE' and stock>=#{quantity}") int reserve(@Param("id")long id,@Param("quantity")int quantity);
    @Update("update product set stock=stock+#{quantity},version=version+1 where id=#{id}") int restore(@Param("id")long id,@Param("quantity")int quantity);
}
