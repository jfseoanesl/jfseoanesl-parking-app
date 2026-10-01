package com.parking.domain.model;

import java.time.Instant;

/** Fila unica por instalacion (03-domain-model.md §Configuration). BR-026 (zona horaria/moneda). */
public record Configuration(
        String id,
        String parkingName,
        String nit,
        String address,
        String phone,
        Integer maxCapacity,
        String timezone,
        String currency,
        String receiptFormatJson,
        Instant updatedAt,
        String updatedBy
) {
}
