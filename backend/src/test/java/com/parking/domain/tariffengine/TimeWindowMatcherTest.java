package com.parking.domain.tariffengine;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeWindowMatcherTest {

    @Test
    void dentroDeFranjaSimpleEsAplicable() {
        assertTrue(TimeWindowMatcher.isWithin(LocalTime.of(10, 0), LocalTime.of(6, 0), LocalTime.of(18, 0)));
    }

    @Test
    void fueraDeFranjaSimpleNoEsAplicable() {
        assertFalse(TimeWindowMatcher.isWithin(LocalTime.of(20, 0), LocalTime.of(6, 0), LocalTime.of(18, 0)));
    }

    @Test
    void limiteInicialEsInclusivo() {
        assertTrue(TimeWindowMatcher.isWithin(LocalTime.of(6, 0), LocalTime.of(6, 0), LocalTime.of(18, 0)));
    }

    @Test
    void limiteFinalEsExclusivo() {
        assertFalse(TimeWindowMatcher.isWithin(LocalTime.of(18, 0), LocalTime.of(6, 0), LocalTime.of(18, 0)));
    }

    @Test
    void franjaQueCruzaMedianocheAplicaAntesDeMedianoche() {
        // PM: 18:00 - 06:00
        assertTrue(TimeWindowMatcher.isWithin(LocalTime.of(23, 0), LocalTime.of(18, 0), LocalTime.of(6, 0)));
    }

    @Test
    void franjaQueCruzaMedianocheAplicaDespuesDeMedianoche() {
        assertTrue(TimeWindowMatcher.isWithin(LocalTime.of(3, 0), LocalTime.of(18, 0), LocalTime.of(6, 0)));
    }

    @Test
    void franjaQueCruzaMedianocheNoAplicaEnHorarioDiurno() {
        assertFalse(TimeWindowMatcher.isWithin(LocalTime.of(12, 0), LocalTime.of(18, 0), LocalTime.of(6, 0)));
    }

    @Test
    void franjaDe24HorasSiempreAplica() {
        assertTrue(TimeWindowMatcher.isWithin(LocalTime.of(0, 0), LocalTime.of(6, 0), LocalTime.of(6, 0)));
        assertTrue(TimeWindowMatcher.isWithin(LocalTime.of(23, 59), LocalTime.of(6, 0), LocalTime.of(6, 0)));
    }

    @Test
    void sinFranjaConfiguradaNuncaAplica() {
        assertFalse(TimeWindowMatcher.isWithin(LocalTime.of(10, 0), null, null));
    }
}
