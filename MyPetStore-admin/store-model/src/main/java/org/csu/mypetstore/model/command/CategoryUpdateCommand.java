package org.csu.mypetstore.model.command;

public record CategoryUpdateCommand(String name, Integer sortOrder) {
}
