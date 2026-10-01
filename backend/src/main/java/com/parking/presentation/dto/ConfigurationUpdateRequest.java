package com.parking.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ConfigurationUpdateRequest(
        @NotBlank(message = "el nombre del parqueadero es obligatorio") String parkingName,
        String nit,
        String address,
        String phone,
        @PositiveOrZero(message = "la capacidad máxima no puede ser negativa") Integer maxCapacity,
        String timezone,
        String currency,
        String receiptFormatJson
) {
}
