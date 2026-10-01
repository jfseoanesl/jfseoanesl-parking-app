package com.parking.domain.model;

import java.time.Instant;

/** Ver 03-domain-model.md §User. BR-003 (RBAC), BR-004 (hash de contraseña). */
public record User(
        String id,
        String username,
        String passwordHash,
        String fullName,
        String roleId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
