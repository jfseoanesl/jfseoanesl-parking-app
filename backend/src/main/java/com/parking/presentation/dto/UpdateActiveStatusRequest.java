package com.parking.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateActiveStatusRequest(@NotNull(message = "el estado activo es obligatorio") Boolean active) {
}
