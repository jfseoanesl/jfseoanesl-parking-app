package com.parking.presentation.dto;

import com.parking.domain.tariffengine.TariffCalculationResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * UC-005. Ver openapi.yaml #TariffQuote. Los campos de primer nivel describen la tarifa sugerida
 * (BR-008); {@code options} lista todas las aplicables, sugerida primero (BR-008.1).
 */
public record TariffQuoteResponse(
        String tariffId,
        String tariffName,
        String modality,
        long elapsedMinutes,
        BigDecimal calculatedAmount,
        List<TariffOption> options,
        /** BR-030: mensualidad de la sesion (null = cliente normal). Si covered, no hay cobro. */
        SubscriptionCoverage subscription
) {
    public record SubscriptionCoverage(String id, String customerName, String status, String paidUntil,
                                       boolean covered) {
        public static SubscriptionCoverage from(com.parking.application.subscription.SubscriptionService.Coverage c) {
            return c == null ? null : new SubscriptionCoverage(c.subscriptionId(), c.customerName(), c.status().name(),
                    c.paidUntil() != null ? c.paidUntil().toString() : null, c.covered());
        }
    }

    public record TariffOption(String tariffId, String tariffName, String modality, BigDecimal calculatedAmount,
                               boolean suggested) {
    }

    /** Salida cubierta: sin tarifa ni valor a cobrar. */
    public static TariffQuoteResponse covered(long elapsedMinutes, SubscriptionCoverage subscription) {
        return new TariffQuoteResponse(null, null, null, elapsedMinutes, BigDecimal.ZERO, List.of(), subscription);
    }

    public static TariffQuoteResponse from(List<TariffCalculationResult> results, SubscriptionCoverage subscription) {
        TariffCalculationResult suggested = results.get(0);
        List<TariffOption> options = results.stream()
                .map(r -> new TariffOption(r.tariffId(), r.tariffName(), r.modality().name(), r.amount(), r == suggested))
                .toList();
        return new TariffQuoteResponse(suggested.tariffId(), suggested.tariffName(), suggested.modality().name(),
                suggested.elapsedMinutes(), suggested.amount(), options, subscription);
    }
}
