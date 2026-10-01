package com.parking.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/** RF-017. */
public record CreateVehicleTypeRequest(
        @NotBlank(message = "el código es obligatorio") String code,
        @NotBlank(message = "el nombre es obligatorio") String name
) {
}
