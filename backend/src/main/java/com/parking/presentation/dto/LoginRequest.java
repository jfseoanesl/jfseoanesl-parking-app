package com.parking.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "el usuario es obligatorio") String username,
        @NotBlank(message = "la contraseña es obligatoria") String password
) {
}
