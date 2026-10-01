package com.parking.domain.tariffengine;

import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.Tariff;

import java.time.Instant;

/**
 * Una implementación por modalidad de tarifa (decisions/ADR-003-motor-tarifario.md). Agregar
 * una modalidad futura (nocturna, festiva) es agregar una clase nueva, sin tocar las existentes.
 */
public interface TariffStrategy {

    boolean isApplicable(ParkingSession session, Tariff tariff, Instant referenceExit);

    TariffCalculationResult calculate(ParkingSession session, Tariff tariff, Instant referenceExit);
}
