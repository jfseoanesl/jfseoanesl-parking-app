package com.parking.presentation.dto;

import com.parking.domain.model.PaymentMethodCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** RF-014. */
public record CreatePaymentMethodRequest(
        @NotBlank(message = "el código es obligatorio") String code,
        @NotBlank(message = "el nombre es obligatorio") String name,
        @NotNull(message = "la categoría es obligatoria") PaymentMethodCategory category
) {
}
