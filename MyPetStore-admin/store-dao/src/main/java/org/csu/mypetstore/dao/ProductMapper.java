package org.csu.mypetstore.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.csu.mypetstore.model.Product;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
