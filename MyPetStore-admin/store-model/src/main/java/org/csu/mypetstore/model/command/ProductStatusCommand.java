package org.csu.mypetstore.model.command;

public record ProductStatusCommand(String status, Long version) {
}
