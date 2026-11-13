package org.csu.mypetstore.service;

public class OrderStateException extends RuntimeException {
    public OrderStateException(String message) {
        super(message);
    }
}
