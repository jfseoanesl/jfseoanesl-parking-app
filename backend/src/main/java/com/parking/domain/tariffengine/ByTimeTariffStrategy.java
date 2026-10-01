package com.parking.domain.tariffengine;

import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.Tariff;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Modalidad BY_TIME: siempre aplicable (no tiene franja horaria propia); cobra por fracciones. */
@Component
public class ByTimeTariffStrategy implements TariffStrategy {

    @Override
    public boolean isApplicable(ParkingSession session, Tariff tariff, Instant referenceExit) {
        return true;
    }

    @Override
    public TariffCalculationResult calculate(ParkingSession session, Tariff tariff, Instant referenceExit) {
        long elapsed = StayDurationCalculator.elapsedMinutes(session.entryDateTime(), referenceExit);
        return new TariffCalculationResult(
                tariff.id(), tariff.name(), tariff.modality(), elapsed,
                ByTimeAmountCalculator.calculate(elapsed, tariff));
    }
}
