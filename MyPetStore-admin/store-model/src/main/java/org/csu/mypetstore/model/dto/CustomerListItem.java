package org.csu.mypetstore.model.dto;

import java.time.LocalDateTime;

public record CustomerListItem(Long id, String username, String displayName,
                               String email, String phone, String status,
                               LocalDateTime createdAt) {}
