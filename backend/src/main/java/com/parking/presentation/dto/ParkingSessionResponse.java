package com.parking.presentation.dto;

import com.parking.application.parking.ParkingSessionView;

import java.math.BigDecimal;
import java.time.Instant;

/** Ver openapi.yaml #ParkingSessionSummary / #ParkingSessionDetail (unificados). */
public record ParkingSessionResponse(
        String id,
        String plate,
        String vehicleTypeCode,
        String vehicleTypeName,
        Instant entryDateTime,
        Instant exitDateTime,
        long elapsedMinutes,
        String entryEmployeeName,
        String status,
        String appliedTariffName,
        BigDecimal calculatedAmount,
        String notes,
        String cancelReason,
        String entryNumber
) {
    public static ParkingSessionResponse from(ParkingSessionView view) {
        return new ParkingSessionResponse(
                view.id(), view.plate(), view.vehicleTypeCode(), view.vehicleTypeName(),
                view.entryDateTime(), view.exitDateTime(), view.elapsedMinutes(), view.entryEmployeeName(),
                view.status().name(), view.appliedTariffName(), view.calculatedAmount(), view.notes(),
                view.cancelReason(), view.entryNumber());
    }
}
