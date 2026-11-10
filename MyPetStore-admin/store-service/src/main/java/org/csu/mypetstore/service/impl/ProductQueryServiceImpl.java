package org.csu.mypetstore.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.dao.ProductMapper;
import org.csu.mypetstore.model.dto.ProductListItem;
import org.csu.mypetstore.model.query.ProductListQuery;
import org.csu.mypetstore.service.ProductQueryService;
import org.springframework.stereotype.Service;

@Service
public class ProductQueryServiceImpl implements ProductQueryService {

    private static final long PAGE_SIZE = 10L;

    private final ProductMapper productMapper;

    public ProductQueryServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public IPage<ProductListItem> findProducts(ProductListQuery query) {
        Page<ProductListItem> page = new Page<>(query.normalizedPage(), PAGE_SIZE);
        return productMapper.selectProductPage(
                page,
                query.normalizedKeyword(),
                query.normalizedStatus());
    }
}
