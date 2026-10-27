package com.parking.presentation.dto;

import com.parking.domain.model.CashSession;

import java.math.BigDecimal;
import java.time.Instant;

/** Ver openapi.yaml #CashSession. Cubre tanto una caja abierta como una ya cerrada. */
public record CashSessionResponse(
        String id,
        String employeeId,
        String status,
        Instant openedAt,
        BigDecimal initialBalance,
        Instant closedAt,
        BigDecimal cashCollected,
        BigDecimal electronicCollected,
        BigDecimal otherCollected,
        BigDecimal totalCollected,
        Integer vehiclesServed,
        BigDecimal expectedCash,
        BigDecimal reportedCash,
        BigDecimal difference,
        BigDecimal otherIncomeCollected,
        BigDecimal expensesTotal
) {
    public static CashSessionResponse from(CashSession session) {
        return new CashSessionResponse(
                session.id(), session.employeeId(), session.status().name(), session.openedAt(),
                session.initialBalance(), session.closedAt(), session.cashCollected(),
                session.electronicCollected(), session.otherCollected(), session.totalCollected(),
                session.vehiclesServed(), session.expectedCash(), session.reportedCash(), session.difference(),
                session.otherIncomeCollected(), session.expensesTotal());
    }
}
