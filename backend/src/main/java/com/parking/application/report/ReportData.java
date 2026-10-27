package com.parking.application.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** RF-060 a RF-063. Indicadores mínimos de prompt.md §13. */
public record ReportData(
        LocalDate from,
        LocalDate to,
        long vehiclesEntered,
        long vehiclesFinalized,
        Map<String, Long> enteredByVehicleType,
        BigDecimal totalCollected,
        Map<String, BigDecimal> collectedByPaymentMethod,
        Map<String, BigDecimal> collectedByTariff,
        Double averageDurationMinutes,
        Integer peakHour,
        Map<String, BigDecimal> collectedByEmployee,
        long cancelledSessions,
        BigDecimal cashDifferencesSum,
        /** BR-031: mensualidades cobradas en el periodo (incluidas en totalCollected). */
        BigDecimal subscriptionCollected,
        long subscriptionPayments,
        /** BR-030: salidas sin cobro de clientes mensuales. */
        long subscriberExits,
        /** RF-083: otros ingresos activos del periodo (incluidos en totalCollected y en los desgloses). */
        BigDecimal otherIncomeCollected,
        /** RF-083: gastos de caja activos del periodo y neto = totalCollected - expensesTotal. */
        BigDecimal expensesTotal,
        BigDecimal netCollected
) {
}
