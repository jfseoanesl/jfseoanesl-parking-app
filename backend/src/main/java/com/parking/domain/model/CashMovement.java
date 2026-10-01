package com.parking.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Ledger auditable de todo lo que afecta el efectivo de una caja (03-domain-model.md §CashMovement). */
public record CashMovement(
        String id,
        String cashSessionId,
        String paymentId,
        CashMovementType type,
        BigDecimal amount,
        String description,
        Instant createdAt
) {
}
