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
        long openCashSessions,
        List<RecentMovement> recentMovements
) {
    public record RecentMovement(String plate, String status, String description) {
    }
}
