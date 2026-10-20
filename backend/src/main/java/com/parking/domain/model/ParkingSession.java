package com.parking.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ver 03-domain-model.md §ParkingSession.
 * BR-001 (unicidad de placa activa), BR-020 (no finalizar sin pago), BR-024 (no borrado fisico).
 */
public record ParkingSession(
        String id,
        String plate,
        String vehicleTypeId,
        Instant entryDateTime,
        Instant exitDateTime,
        ParkingStatus status,
        String entryEmployeeId,
        String exitEmployeeId,
        String appliedTariffId,
        BigDecimal calculatedAmount,
        String notes,
        String cancelReason,
        String cancelledBy,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt,
        /** Consecutivo del comprobante de ingreso (ING-000001, BR-027); null en sesiones anteriores a V6. */
        String entryNumber,
        /** BR-029: mensualidad que cubre la sesión; null = cliente normal. */
        String subscriptionId
) {
}
