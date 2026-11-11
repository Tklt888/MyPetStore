package org.csu.mypetstore.model.dto;

public record CategoryEditView(
        Long id,
        String code,
        Long parentId,
        String parentName,
        String name,
        Integer sortOrder) {
}
