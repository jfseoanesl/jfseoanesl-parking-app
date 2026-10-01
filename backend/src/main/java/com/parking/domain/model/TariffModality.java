package com.parking.domain.model;

/** Modalidades de tarifa soportadas (prompt.md §9). Ver decisions/ADR-003-motor-tarifario.md. */
public enum TariffModality {
    FULL_AM,
    FULL_PM,
    FULL_DAY,
    BY_TIME
}
