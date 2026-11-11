package org.csu.mypetstore.service;

public class ProductConflictException extends RuntimeException {
    public ProductConflictException() {
        super("商品已发生变化，请刷新后重试");
    }
}
