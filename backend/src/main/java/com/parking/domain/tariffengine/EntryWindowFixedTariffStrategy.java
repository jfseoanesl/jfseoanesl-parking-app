package com.parking.domain.tariffengine;

import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.Tariff;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Modalidad FULL_DAY (BR-013): aplica si el INGRESO cae dentro de la franja horaria de la
 * tarifa. Valor fijo, independiente de la duración de la permanencia.
 */
@Component
public class EntryWindowFixedTariffStrategy implements TariffStrategy {

    private final TimeProvider timeProvider;

    public EntryWindowFixedTariffStrategy(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Override
    public boolean isApplicable(ParkingSession session, Tariff tariff, Instant referenceExit) {
        LocalTime entryTime = session.entryDateTime().atZone(timeProvider.zone()).toLocalTime();
        return TimeWindowMatcher.isWithin(entryTime, tariff.startTime(), tariff.endTime());
    }

    @Override
    public TariffCalculationResult calculate(ParkingSession session, Tariff tariff, Instant referenceExit) {
        long elapsed = StayDurationCalculator.elapsedMinutes(session.entryDateTime(), referenceExit);
        return new TariffCalculationResult(tariff.id(), tariff.name(), tariff.modality(), elapsed, tariff.value());
    }
}
