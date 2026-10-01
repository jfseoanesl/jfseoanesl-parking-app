package com.parking.domain.tariffengine;

import java.time.LocalTime;

/** BR-010: franjas horarias configurables por tarifa, incluyendo franjas que cruzan medianoche. */
public final class TimeWindowMatcher {

    private TimeWindowMatcher() {
    }

    /** [start, end) - si start > end, la franja cruza medianoche (ej. PM: 18:00-06:00). */
    public static boolean isWithin(LocalTime time, LocalTime start, LocalTime end) {
        if (start == null || end == null) return false;
        if (start.equals(end)) return true; // franja de 24h
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }
}
