package com.parking.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** RF-077 / BR-031: pago mensual cobrado en caja; cubre [periodFrom, periodTo]. */
public record SubscriptionPayment(
        String id,
        String subscriptionId,
        String cashSessionId,
        String paymentMethodId,
        String userId,
        BigDecimal amount,
        LocalDate periodFrom,
        LocalDate periodTo,
        String operationNumber,
        Instant paidAt
) {
}
