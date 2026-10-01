package com.parking.domain.tariffengine;

import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.Tariff;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Modalidades FULL_AM y FULL_PM (BR-014): aplica si la SALIDA (o el instante de referencia
 * para una cotización) cae dentro de la franja horaria de la tarifa. Valor fijo. Ambas
 * modalidades comparten exactamente este comportamiento — la franja concreta (mañana/tarde)
 * es un dato de la tarifa, no de la clase — por eso una sola estrategia las sirve a las dos
 * (ver TariffStrategyFactory); crear dos clases idénticas sería duplicación sin propósito.
 */
@Component
public class ExitWindowFixedTariffStrategy implements TariffStrategy {

    private final TimeProvider timeProvider;

    public ExitWindowFixedTariffStrategy(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Override
    public boolean isApplicable(ParkingSession session, Tariff tariff, Instant referenceExit) {
        LocalTime exitTime = referenceExit.atZone(timeProvider.zone()).toLocalTime();
        return TimeWindowMatcher.isWithin(exitTime, tariff.startTime(), tariff.endTime());
    }

    @Override
    public TariffCalculationResult calculate(ParkingSession session, Tariff tariff, Instant referenceExit) {
        long elapsed = StayDurationCalculator.elapsedMinutes(session.entryDateTime(), referenceExit);
        return new TariffCalculationResult(tariff.id(), tariff.name(), tariff.modality(), elapsed, tariff.value());
    }
}
