package com.parking.domain.model;

import java.time.LocalDate;

/** BR-028 / BR-031: reglas de fechas de las mensualidades, sin dependencias (probadas en unitario). */
public final class SubscriptionPeriods {

    private SubscriptionPeriods() {
    }

    public static SubscriptionStatus statusOf(boolean active, LocalDate paidUntil, int graceDays, LocalDate today) {
        if (!active) return SubscriptionStatus.INACTIVA;
        if (paidUntil == null) return SubscriptionStatus.SIN_PAGO;
        if (!today.isAfter(paidUntil)) return SubscriptionStatus.AL_DIA;
        if (!today.isAfter(paidUntil.plusDays(graceDays))) return SubscriptionStatus.EN_GRACIA;
        return SubscriptionStatus.VENCIDA;
    }

    /**
     * Inicio del periodo que cubre un pago nuevo: el día siguiente al último cubierto, o hoy si nunca
     * pagó o ya venció (no se cobran retroactivamente los días sin cobertura).
     */
    public static LocalDate nextPeriodStart(LocalDate paidUntil, LocalDate today) {
        if (paidUntil == null || paidUntil.isBefore(today)) return today;
        return paidUntil.plusDays(1);
    }

    /**
     * Fin del periodo: la primera fecha con día = corte estrictamente posterior al inicio
     * (corte 5, inicio 19-oct → 5-nov; inicio 6-nov → 5-dic). El corte va de 1 a 28, así que existe
     * en todos los meses.
     */
    public static LocalDate periodEnd(LocalDate periodStart, int cutOffDay) {
        LocalDate candidate = periodStart.withDayOfMonth(cutOffDay);
        return candidate.isAfter(periodStart) ? candidate : candidate.plusMonths(1);
    }
}
