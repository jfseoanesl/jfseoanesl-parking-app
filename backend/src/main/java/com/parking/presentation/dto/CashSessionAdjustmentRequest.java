package com.parking.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** RF-057/BR-023: corrección administrativa de un cierre de caja finalizado. */
public record CashSessionAdjustmentRequest(
        @NotNull(message = "el nuevo efectivo reportado es obligatorio")
        @DecimalMin(value = "0", message = "el efectivo reportado no puede ser negativo") BigDecimal reportedCash,
        @NotBlank(message = "el motivo de la corrección es obligatorio") String reason
) {
}
