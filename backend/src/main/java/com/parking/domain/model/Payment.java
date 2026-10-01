package com.parking.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Ver 03-domain-model.md §Payment. BR-017 (sin datos bancarios), BR-021 (idempotencia). */
public record Payment(
        String id,
        String parkingSessionId,
        String cashSessionId,
        BigDecimal calculatedAmount,
        BigDecimal chargedAmount,
        String paymentMethodId,
        String operationNumber,
        String userId,
        Instant paidAt,
        Instant createdAt
) {
}
