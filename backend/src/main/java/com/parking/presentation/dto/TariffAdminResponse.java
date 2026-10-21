package com.parking.presentation.dto;

import com.parking.domain.model.Tariff;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record TariffAdminResponse(
        String id,
        String name,
        String description,
        String vehicleTypeId,
        String modality,
        BigDecimal value,
        BigDecimal firstFractionValue,
        LocalTime startTime,
        LocalTime endTime,
        Integer toleranceMinutes,
        Integer minTimeMinutes,
        String timeUnit,
        Integer fractionMinutes,
        String rounding,
        int priority,
        boolean active,
        LocalDate validFrom,
        LocalDate validTo,
        Instant createdAt,
        Instant updatedAt
) {
    public static TariffAdminResponse from(Tariff t) {
        return new TariffAdminResponse(t.id(), t.name(), t.description(), t.vehicleTypeId(), t.modality().name(),
                t.value(), t.firstFractionValue(), t.startTime(), t.endTime(), t.toleranceMinutes(), t.minTimeMinutes(),
                t.timeUnit() != null ? t.timeUnit().name() : null, t.fractionMinutes(), t.rounding().name(),
                t.priority(), t.active(), t.validFrom(), t.validTo(), t.createdAt(), t.updatedAt());
    }
}
