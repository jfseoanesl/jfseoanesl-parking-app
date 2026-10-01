package com.parking.domain.model;

/** Catalogo parametrizable de medios de pago (BR-018). Semilla: CASH, TRANSFER, OTHER. */
public record PaymentMethod(String id, String code, String name, PaymentMethodCategory category, boolean active) {
}
