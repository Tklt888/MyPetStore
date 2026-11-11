package org.csu.mypetstore.model.query;

public class CategoryListQuery {

    public static final long PAGE_SIZE = 10L;

    private Long page;

    public long getPage() {
        return page == null || page < 1 ? 1L : page;
    }

    public void setPage(Long page) {
        this.page = page;
    }
}
