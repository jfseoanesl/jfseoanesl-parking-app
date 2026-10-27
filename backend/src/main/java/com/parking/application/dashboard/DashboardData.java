package com.parking.application.dashboard;

import java.math.BigDecimal;
import java.util.List;

/** RF-064/RF-065. */
public record DashboardData(
        long occupied,
        Integer maxCapacity,
        long entriesToday,
        long exitsToday,
        BigDecimal collectedToday,
        /** RF-084: otros ingresos de hoy (ya incluidos en collectedToday) y gastos de caja de hoy. */
        BigDecimal otherIncomeToday,
        BigDecimal expensesToday,
        long openCashSessions,
        List<RecentMovement> recentMovements
) {
    public record RecentMovement(String plate, String status, String description) {
    }
}
