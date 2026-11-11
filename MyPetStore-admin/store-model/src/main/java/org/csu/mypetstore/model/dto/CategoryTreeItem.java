package org.csu.mypetstore.model.dto;

import java.util.List;

public record CategoryTreeItem(
        Long id,
        String code,
        String name,
        Integer sortOrder,
        List<CategoryRow> children) {

    public CategoryTreeItem {
        children = List.copyOf(children);
    }
}
