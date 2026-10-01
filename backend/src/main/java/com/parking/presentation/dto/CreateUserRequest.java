package com.parking.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** RF-005. */
public record CreateUserRequest(
        @NotBlank(message = "el usuario es obligatorio") String username,
        @NotBlank(message = "el nombre completo es obligatorio") String fullName,
        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 8, message = "la contraseña debe tener al menos 8 caracteres") String password,
        @NotBlank(message = "el rol es obligatorio")
        @Pattern(regexp = "ADMIN|EMPLOYEE", message = "rol inválido") String role
) {
}
