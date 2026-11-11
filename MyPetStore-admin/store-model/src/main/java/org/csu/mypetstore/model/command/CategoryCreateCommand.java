package org.csu.mypetstore.model.command;

public record CategoryCreateCommand(String code, String name, Integer sortOrder, Long parentId) {
}
