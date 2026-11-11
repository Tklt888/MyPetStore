package org.csu.mypetstore.service;

public class CategoryValidationException extends RuntimeException {

    private final String field;

    public CategoryValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
