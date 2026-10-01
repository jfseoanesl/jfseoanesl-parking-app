package com.parking.presentation.dto;

import com.parking.application.parking.CheckoutResult;

import java.math.BigDecimal;
import java.time.Instant;

/** UC-004. Ver openapi.yaml #CheckoutResult. */
public record CheckoutResultResponse(
        String sessionId,
        String paymentId,
        String plate,
        Instant entryDateTime,
        Instant exitDateTime,
        String appliedTariffName,
        String modality,
        BigDecimal calculatedAmount,
        BigDecimal chargedAmount,
        String paymentMethodName,
        String operationNumber,
        String status
) {
    public static CheckoutResultResponse from(CheckoutResult result) {
        return new CheckoutResultResponse(
                result.session().id(),
                result.payment().id(),
                result.session().plate(),
                result.session().entryDateTime(),
                result.session().exitDateTime(),
                result.tariffResult().tariffName(),
                result.tariffResult().modality().name(),
                result.tariffResult().amount(),
                result.payment().chargedAmount(),
                result.paymentMethod().name(),
                result.payment().operationNumber(),
                result.session().status().name()
        );
    }
}
