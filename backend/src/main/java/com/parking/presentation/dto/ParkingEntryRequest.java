package com.parking.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/** RF-023. Ver openapi.yaml #ParkingEntryRequest. */
public record ParkingEntryRequest(
        @NotBlank(message = "la placa es obligatoria") String plate,
        @NotBlank(message = "el tipo de vehículo es obligatorio") String vehicleTypeId,
        String notes
) {
}
