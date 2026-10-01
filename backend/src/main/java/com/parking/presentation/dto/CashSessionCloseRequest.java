package com.parking.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CashSessionCloseRequest(
        @NotNull(message = "el efectivo reportado es obligatorio")
        @DecimalMin(value = "0", message = "el efectivo reportado no puede ser negativo") BigDecimal reportedCash
) {
}
