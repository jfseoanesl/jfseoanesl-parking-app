package com.parking.domain.tariffengine;

import com.parking.domain.model.Tariff;
import com.parking.domain.model.TariffModality;
import com.parking.domain.model.TariffRounding;
import com.parking.domain.model.TariffTimeUnit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** BR-011 / BR-012 — batería de casos de borde del motor tarifario (prompt.md §32). */
class ByTimeAmountCalculatorTest {

    private static Tariff byTime(int toleranceMinutes, Integer minTimeMinutes, int fractionMinutes,
                                  TariffRounding rounding, long valuePerFraction) {
        return new Tariff("t1", "Por tiempo", null, "vt1", TariffModality.BY_TIME,
                BigDecimal.valueOf(valuePerFraction), null, null, toleranceMinutes, minTimeMinutes,
                TariffTimeUnit.MINUTES, fractionMinutes, rounding, 1, true,
                LocalDate.of(2026, 1, 1), null, Instant.now(), Instant.now(), null, null);
    }

    @Test
    void dentroDeTolerenciaNoCobra() {
        Tariff tariff = byTime(10, null, 60, TariffRounding.UP, 1000);
        assertEquals(BigDecimal.ZERO, ByTimeAmountCalculator.calculate(10, tariff));
        assertEquals(BigDecimal.ZERO, ByTimeAmountCalculator.calculate(5, tariff));
    }

    @Test
    void justoDespuesDeTolerenciaCobraFraccionCompletaHaciaArriba() {
        Tariff tariff = byTime(10, null, 60, TariffRounding.UP, 1000);
        // 11 minutos > tolerancia de 10 -> se factura el TOTAL (11 min), redondeado hacia arriba a 1 fraccion de 60.
        assertEquals(BigDecimal.valueOf(1000), ByTimeAmountCalculator.calculate(11, tariff));
    }

    @Test
    void fraccionParcialSiguienteSeCobraCompletaConRedondeoUp() {
        Tariff tariff = byTime(10, null, 60, TariffRounding.UP, 1000);
        assertEquals(BigDecimal.valueOf(2000), ByTimeAmountCalculator.calculate(61, tariff));
        assertEquals(BigDecimal.valueOf(2000), ByTimeAmountCalculator.calculate(119, tariff));
        assertEquals(BigDecimal.valueOf(2000), ByTimeAmountCalculator.calculate(120, tariff));
        assertEquals(BigDecimal.valueOf(3000), ByTimeAmountCalculator.calculate(121, tariff));
    }

    @Test
    void redondeoDownSoloCobraFraccionesCompletas() {
        Tariff tariff = byTime(0, null, 60, TariffRounding.DOWN, 1000);
        assertEquals(BigDecimal.valueOf(1000), ByTimeAmountCalculator.calculate(119, tariff));
        // Con DOWN, una permanencia menor a una fraccion completa (30 < 60) no llega a facturar
        // ninguna fraccion: es una eleccion deliberada del administrador al elegir esta politica.
        assertEquals(BigDecimal.ZERO, ByTimeAmountCalculator.calculate(30, tariff));
    }

    @Test
    void redondeoNearestRedondeaAlMasCercano() {
        Tariff tariff = byTime(0, null, 60, TariffRounding.NEAREST, 1000);
        assertEquals(BigDecimal.valueOf(1000), ByTimeAmountCalculator.calculate(89, tariff)); // 1.48 -> 1
        assertEquals(BigDecimal.valueOf(2000), ByTimeAmountCalculator.calculate(91, tariff)); // 1.52 -> 2
    }

    @Test
    void tiempoMinimoActuaComoPisoDeFacturacion() {
        Tariff tariff = byTime(5, 30, 60, TariffRounding.UP, 1000);
        // 6 minutos (> tolerancia de 5) pero por debajo del piso de 30 -> se factura como si fueran 30.
        assertEquals(BigDecimal.valueOf(1000), ByTimeAmountCalculator.calculate(6, tariff));
    }

    @Test
    void sinFraccionConfiguradaUsaUnMinutoComoFraccion() {
        Tariff tariff = byTime(0, null, 0, TariffRounding.UP, 10);
        assertEquals(BigDecimal.valueOf(50), ByTimeAmountCalculator.calculate(5, tariff));
    }
}
