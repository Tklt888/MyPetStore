package org.csu.mypetstore.service.impl;

import org.csu.mypetstore.dao.ProductMapper;
import org.csu.mypetstore.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public long countProducts() {
        return productMapper.selectCount(null);
    }
}
