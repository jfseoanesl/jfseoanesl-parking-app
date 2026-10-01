package com.parking.presentation.dto;

import com.parking.domain.model.TariffModality;
import com.parking.domain.model.TariffRounding;
import com.parking.domain.model.TariffTimeUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** RF-018/RF-019. Ver openapi.yaml #TariffInput. */
public record TariffAdminRequest(
        @NotBlank(message = "el nombre es obligatorio") String name,
        String description,
        @NotBlank(message = "el tipo de vehículo es obligatorio") String vehicleTypeId,
        @NotNull(message = "la modalidad es obligatoria") TariffModality modality,
        @NotNull(message = "el valor es obligatorio") BigDecimal value,
        LocalTime startTime,
        LocalTime endTime,
        Integer toleranceMinutes,
        Integer minTimeMinutes,
        TariffTimeUnit timeUnit,
        Integer fractionMinutes,
        TariffRounding rounding,
        Integer priority,
        Boolean active,
        LocalDate validFrom,
        LocalDate validTo
) {
}
