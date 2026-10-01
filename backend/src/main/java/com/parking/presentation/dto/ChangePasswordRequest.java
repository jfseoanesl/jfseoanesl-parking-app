package com.parking.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "la nueva contraseña es obligatoria")
        @Size(min = 8, message = "la contraseña debe tener al menos 8 caracteres") String newPassword
) {
}
