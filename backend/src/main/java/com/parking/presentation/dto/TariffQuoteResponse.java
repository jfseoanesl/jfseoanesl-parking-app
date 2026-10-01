package com.parking.presentation.dto;

import com.parking.domain.tariffengine.TariffCalculationResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * UC-005. Ver openapi.yaml #TariffQuote. Los campos de primer nivel describen la tarifa sugerida
 * (BR-008); {@code options} lista todas las aplicables, sugerida primero (BR-008.1).
 */
public record TariffQuoteResponse(
        String tariffId,
        String tariffName,
        String modality,
        long elapsedMinutes,
        BigDecimal calculatedAmount,
        List<TariffOption> options
) {
    public record TariffOption(String tariffId, String tariffName, String modality, BigDecimal calculatedAmount,
                               boolean suggested) {
    }

    public static TariffQuoteResponse from(List<TariffCalculationResult> results) {
        TariffCalculationResult suggested = results.get(0);
        List<TariffOption> options = results.stream()
                .map(r -> new TariffOption(r.tariffId(), r.tariffName(), r.modality().name(), r.amount(), r == suggested))
                .toList();
        return new TariffQuoteResponse(suggested.tariffId(), suggested.tariffName(), suggested.modality().name(),
                suggested.elapsedMinutes(), suggested.amount(), options);
    }
}
