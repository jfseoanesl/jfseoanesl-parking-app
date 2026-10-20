package com.parking.presentation.dto;

import com.parking.application.report.ReportData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** RF-060 a RF-063. Ver 08-api-contract.md. */
public record ReportResponse(
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
        BigDecimal subscriptionCollected,
        long subscriptionPayments,
        long subscriberExits
) {
    public static ReportResponse from(ReportData d) {
        return new ReportResponse(d.from(), d.to(), d.vehiclesEntered(), d.vehiclesFinalized(),
                d.enteredByVehicleType(), d.totalCollected(), d.collectedByPaymentMethod(), d.collectedByTariff(),
                d.averageDurationMinutes(), d.peakHour(), d.collectedByEmployee(), d.cancelledSessions(),
                d.cashDifferencesSum(), d.subscriptionCollected(), d.subscriptionPayments(), d.subscriberExits());
    }
}
