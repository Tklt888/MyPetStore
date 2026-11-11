package org.csu.mypetstore.model.dto;

public record AdminAccount(
        Long id,
        String username,
        String passwordHash,
        String displayName,
        String role,
        String status) {
}
