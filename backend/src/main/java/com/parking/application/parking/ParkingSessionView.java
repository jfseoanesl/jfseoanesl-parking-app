package com.parking.application.parking;

import com.parking.domain.model.ParkingStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Modelo de lectura para RF-030 a RF-033 (vehículos activos) y detalle de sesión — sirve tanto
 * al listado como al detalle; los campos que no aplican a una vista quedan en null.
 */
public record ParkingSessionView(
        String id,
        String plate,
        String vehicleTypeCode,
        String vehicleTypeName,
        Instant entryDateTime,
        Instant exitDateTime,
        long elapsedMinutes,
        String entryEmployeeName,
        ParkingStatus status,
        String appliedTariffName,
        BigDecimal calculatedAmount,
        String notes,
        String cancelReason,
        String entryNumber
) {
}
