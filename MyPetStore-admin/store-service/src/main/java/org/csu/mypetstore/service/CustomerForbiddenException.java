package org.csu.mypetstore.service;

public class CustomerForbiddenException extends RuntimeException {
    public CustomerForbiddenException() {
        super("只能修改客户账号状态");
    }
}
