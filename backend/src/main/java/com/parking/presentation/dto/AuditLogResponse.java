package com.parking.presentation.dto;

import java.time.Instant;

/** RF-067. Ver 08-api-contract.md §Auditoría. */
public record AuditLogResponse(
        String id,
        String userId,
        String userFullName,
        String action,
        String entityName,
        String entityId,
        String oldValue,
        String newValue,
        Instant occurredAt
) {
}
