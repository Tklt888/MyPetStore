package org.csu.mypetstore.model.command;

public record ProductRestockCommand(Integer quantity, Long version) {
}
