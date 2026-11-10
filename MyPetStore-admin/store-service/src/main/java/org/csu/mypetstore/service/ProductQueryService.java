package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.csu.mypetstore.model.dto.ProductListItem;
import org.csu.mypetstore.model.query.ProductListQuery;

public interface ProductQueryService {

    IPage<ProductListItem> findProducts(ProductListQuery query);
}
