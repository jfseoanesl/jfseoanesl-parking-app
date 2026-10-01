package com.parking.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelSessionRequest(@NotBlank(message = "el motivo es obligatorio") String reason) {
}
