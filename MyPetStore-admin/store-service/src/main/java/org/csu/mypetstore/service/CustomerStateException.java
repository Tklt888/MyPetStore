package org.csu.mypetstore.service;

public class CustomerStateException extends RuntimeException {
    public CustomerStateException(String message) {
        super(message);
    }
}
