package org.csu.mypetstore.service;

public class ProductValidationException extends RuntimeException {
    private final String field;

    public ProductValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
