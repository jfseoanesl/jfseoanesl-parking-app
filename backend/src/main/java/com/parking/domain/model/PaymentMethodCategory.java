package com.parking.domain.model;

/** Clasificación para el arqueo de caja (RF-055): solo CASH afecta el efectivo físico esperado. */
public enum PaymentMethodCategory {
    CASH,
    ELECTRONIC,
    OTHER
}
