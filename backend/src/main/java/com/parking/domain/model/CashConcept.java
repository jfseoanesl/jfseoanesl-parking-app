package com.parking.domain.model;

/** RF-079: concepto del catálogo de otros ingresos o gastos, administrado por el administrador. */
public record CashConcept(
        String id,
        CashTransactionType type,
        String name,
        boolean active
) {
}
