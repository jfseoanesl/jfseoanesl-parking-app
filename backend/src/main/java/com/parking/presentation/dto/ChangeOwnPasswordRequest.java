package com.parking.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio de la PROPIA contrasena: exige la actual (revision OWASP A07, 2026-09-29), para que quien
 * tome una sesion abierta ajena no pueda quedarse con la cuenta cambiandola.
 */
public record ChangeOwnPasswordRequest(
        @NotBlank(message = "la contraseña actual es obligatoria") String currentPassword,
        @NotBlank(message = "la nueva contraseña es obligatoria")
        @Size(min = 8, message = "la contraseña debe tener al menos 8 caracteres") String newPassword
) {
}
