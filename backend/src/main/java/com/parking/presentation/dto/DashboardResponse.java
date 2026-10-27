package com.parking.presentation.dto;

import com.parking.application.dashboard.DashboardData;

import java.math.BigDecimal;
import java.util.List;

/** RF-064/RF-065. */
public record DashboardResponse(
        long occupied,
        Integer maxCapacity,
        long entriesToday,
        long exitsToday,
        BigDecimal collectedToday,
        BigDecimal otherIncomeToday,
        BigDecimal expensesToday,
        long openCashSessions,
        List<DashboardData.RecentMovement> recentMovements
) {
    public static DashboardResponse from(DashboardData d) {
        return new DashboardResponse(d.occupied(), d.maxCapacity(), d.entriesToday(), d.exitsToday(),
                d.collectedToday(), d.otherIncomeToday(), d.expensesToday(), d.openCashSessions(), d.recentMovements());
    }
}
