package com.parking.presentation.dto;

import com.parking.application.cash.CashSessionSummary;

import java.math.BigDecimal;

/** Ver openapi.yaml #CashSessionSummary. */
public record CashSessionSummaryResponse(
        BigDecimal initialBalance,
        BigDecimal cashCollected,
        BigDecimal electronicCollected,
        BigDecimal otherCollected,
        BigDecimal totalCollected,
        int vehiclesServed,
        BigDecimal expectedCash,
        BigDecimal otherIncomeCollected,
        BigDecimal expensesTotal,
        BigDecimal expensesCash
) {
    public static CashSessionSummaryResponse from(CashSessionSummary summary) {
        return new CashSessionSummaryResponse(summary.initialBalance(), summary.cashCollected(),
                summary.electronicCollected(), summary.otherCollected(), summary.totalCollected(),
                summary.vehiclesServed(), summary.expectedCash(), summary.otherIncomeCollected(),
                summary.expensesTotal(), summary.expensesCash());
    }
}
