package com.parking.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Ver 03-domain-model.md §CashSession. BR-022 (unica caja abierta por empleado). */
public record CashSession(
        String id,
        String employeeId,
        CashSessionStatus status,
        Instant openedAt,
        BigDecimal initialBalance,
        Instant closedAt,
        BigDecimal cashCollected,
        BigDecimal electronicCollected,
        BigDecimal otherCollected,
        BigDecimal totalCollected,
        Integer vehiclesServed,
        BigDecimal expectedCash,
        BigDecimal reportedCash,
        BigDecimal difference,
        Instant createdAt,
        Instant updatedAt
) {
}
