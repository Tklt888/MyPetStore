package org.csu.mypetstore.service;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(long id) {
        super("订单不存在：" + id);
    }
}
