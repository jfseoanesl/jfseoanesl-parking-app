package com.parking.domain.model;

import java.time.Instant;

/** Ver 03-domain-model.md §AuditLog. BR-025 (obligatorio), BR-024 (solo append, nunca se edita/borra). */
public record AuditLog(
        String id,
        String userId,
        String action,
        String entityName,
        String entityId,
        String oldValue,
        String newValue,
        Instant occurredAt
) {
}
