package com.parking.domain.tariffengine;

import java.time.Duration;
import java.time.Instant;

/**
 * BR-014: el tiempo de permanencia es continuo - no se recalcula por cruzar medianoche ni
 * cambiar de dia calendario. Es una simple resta de instantes.
 */
public final class StayDurationCalculator {

    private StayDurationCalculator() {
    }

    public static long elapsedMinutes(Instant entryDateTime, Instant referenceExit) {
        return Duration.between(entryDateTime, referenceExit).toMinutes();
    }
}
