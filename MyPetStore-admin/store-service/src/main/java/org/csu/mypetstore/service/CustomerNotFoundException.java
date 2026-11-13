package org.csu.mypetstore.service;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(long customerId) {
        super("客户不存在：" + customerId);
    }
}
