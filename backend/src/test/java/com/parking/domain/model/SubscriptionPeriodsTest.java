package com.parking.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** BR-028 (estado) y BR-031 (periodo cubierto por un pago). */
class SubscriptionPeriodsTest {

    private static final LocalDate OCT_19 = LocalDate.of(2026, 10, 19);

    @Test
    void elPeriodoTerminaEnElSiguienteCortePosteriorAlInicio() {
        assertThat(SubscriptionPeriods.periodEnd(OCT_19, 5)).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(SubscriptionPeriods.periodEnd(LocalDate.of(2026, 10, 3), 5)).isEqualTo(LocalDate.of(2026, 10, 5));
        // Inicio el mismo día del corte: cubre hasta el corte del mes siguiente.
        assertThat(SubscriptionPeriods.periodEnd(LocalDate.of(2026, 10, 5), 5)).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(SubscriptionPeriods.periodEnd(LocalDate.of(2026, 12, 20), 10)).isEqualTo(LocalDate.of(2027, 1, 10));
        // Corte 28 existe también en febrero.
        assertThat(SubscriptionPeriods.periodEnd(LocalDate.of(2027, 1, 30), 28)).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    void elPagoAdelantadoContinuaDesdeElUltimoDiaCubierto() {
        LocalDate paidUntil = LocalDate.of(2026, 11, 5);
        LocalDate start = SubscriptionPeriods.nextPeriodStart(paidUntil, LocalDate.of(2026, 11, 1));
        assertThat(start).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(SubscriptionPeriods.periodEnd(start, 5)).isEqualTo(LocalDate.of(2026, 12, 5));
    }

    @Test
    void sinPagoPrevioOVencidaElPeriodoEmpiezaHoy() {
        assertThat(SubscriptionPeriods.nextPeriodStart(null, OCT_19)).isEqualTo(OCT_19);
        assertThat(SubscriptionPeriods.nextPeriodStart(LocalDate.of(2026, 9, 5), OCT_19)).isEqualTo(OCT_19);
    }

    @Test
    void estadoSegunFechaDeCorteYDiasDeGracia() {
        LocalDate paidUntil = LocalDate.of(2026, 10, 5);
        assertThat(SubscriptionPeriods.statusOf(true, paidUntil, 3, LocalDate.of(2026, 10, 5))).isEqualTo(SubscriptionStatus.AL_DIA);
        assertThat(SubscriptionPeriods.statusOf(true, paidUntil, 3, LocalDate.of(2026, 10, 8))).isEqualTo(SubscriptionStatus.EN_GRACIA);
        assertThat(SubscriptionPeriods.statusOf(true, paidUntil, 3, LocalDate.of(2026, 10, 9))).isEqualTo(SubscriptionStatus.VENCIDA);
        assertThat(SubscriptionPeriods.statusOf(true, paidUntil, 0, LocalDate.of(2026, 10, 6))).isEqualTo(SubscriptionStatus.VENCIDA);
        assertThat(SubscriptionPeriods.statusOf(true, null, 3, OCT_19)).isEqualTo(SubscriptionStatus.SIN_PAGO);
        assertThat(SubscriptionPeriods.statusOf(false, paidUntil, 3, LocalDate.of(2026, 10, 1))).isEqualTo(SubscriptionStatus.INACTIVA);
        assertThat(SubscriptionStatus.EN_GRACIA.coversExit()).isTrue();
        assertThat(SubscriptionStatus.VENCIDA.coversExit()).isFalse();
    }
}
