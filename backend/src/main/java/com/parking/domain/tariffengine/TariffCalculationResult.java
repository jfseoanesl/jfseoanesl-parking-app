package com.parking.domain.tariffengine;

import com.parking.domain.model.TariffModality;

import java.math.BigDecimal;

/** Resultado del motor de tarifas (RF-043 a RF-048). */
public record TariffCalculationResult(
        String tariffId,
        String tariffName,
        TariffModality modality,
        long elapsedMinutes,
        BigDecimal amount
) {
}
