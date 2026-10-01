package com.parking.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Ver 03-domain-model.md §Tariff y decisions/ADR-003-motor-tarifario.md.
 * Todos los campos de condicion (horario, tolerancia, fraccion, redondeo, prioridad) viven
 * aqui directamente: no existe una entidad TariffRule separada en el MVP.
 */
public record Tariff(
        String id,
        String name,
        String description,
        String vehicleTypeId,
        TariffModality modality,
        BigDecimal value,
        LocalTime startTime,
        LocalTime endTime,
        Integer toleranceMinutes,
        Integer minTimeMinutes,
        TariffTimeUnit timeUnit,
        Integer fractionMinutes,
        TariffRounding rounding,
        int priority,
        boolean active,
        LocalDate validFrom,
        LocalDate validTo,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
