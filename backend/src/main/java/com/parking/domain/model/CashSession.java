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
        /** BR-033: otros ingresos y gastos de la caja; foto al cierre (0 mientras está abierta). */
        BigDecimal otherIncomeCollected,
        BigDecimal expensesTotal,
        Instant createdAt,
        Instant updatedAt
) {
}
