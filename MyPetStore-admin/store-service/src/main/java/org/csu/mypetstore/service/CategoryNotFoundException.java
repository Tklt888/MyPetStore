package org.csu.mypetstore.service;

public class CategoryNotFoundException extends RuntimeException {

    private final long categoryId;

    public CategoryNotFoundException(long categoryId) {
        super("分类不存在");
        this.categoryId = categoryId;
    }

    public long getCategoryId() {
        return categoryId;
    }
}
