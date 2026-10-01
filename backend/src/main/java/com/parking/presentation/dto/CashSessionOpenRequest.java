package com.parking.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CashSessionOpenRequest(
        @NotNull(message = "el saldo inicial es obligatorio")
        @DecimalMin(value = "0", message = "el saldo inicial no puede ser negativo") BigDecimal initialBalance
) {
}
