package com.parking.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** UC-004. Ver openapi.yaml #CheckoutRequest. */
public record CheckoutRequest(
        @NotBlank(message = "el medio de pago es obligatorio") String paymentMethodId,
        @NotNull(message = "el valor cobrado es obligatorio")
        @DecimalMin(value = "0", message = "el valor cobrado no puede ser negativo") BigDecimal chargedAmount,
        /** BR-008.1: tarifa elegida entre las aplicables; opcional (null = la sugerida). */
        String tariffId
) {
}
