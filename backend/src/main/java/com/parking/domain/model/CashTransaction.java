package com.parking.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/** RF-080/RF-081: otro ingreso o gasto registrado en una caja abierta. Ver BR-032 a BR-035. */
public record CashTransaction(
        String id,
        String cashSessionId,
        CashTransactionType type,
        String conceptId,
        String description,
        BigDecimal amount,
        String paymentMethodId,
        String operationNumber,
        String userId,
        Instant createdAt,
        CashTransactionStatus status,
        String voidReason,
        String voidedBy,
        Instant voidedAt
) {
    public boolean isActive() {
        return status == CashTransactionStatus.ACTIVE;
    }
}
