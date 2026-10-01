package com.parking.presentation.dto;

import com.parking.domain.model.PaymentMethod;

public record PaymentMethodResponse(String id, String code, String name) {

    public static PaymentMethodResponse from(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(paymentMethod.id(), paymentMethod.code(), paymentMethod.name());
    }
}
