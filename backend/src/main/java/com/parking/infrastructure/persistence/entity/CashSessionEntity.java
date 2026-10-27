package com.parking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** La columna generada "employee_open" (BR-022) no se mapea: la mantiene la base de datos. */
@Entity
@Table(name = "cash_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashSessionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "employee_id", nullable = false, length = 36)
    private String employeeId;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "initial_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal initialBalance;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "cash_collected", precision = 12, scale = 2)
    private BigDecimal cashCollected;

    @Column(name = "electronic_collected", precision = 12, scale = 2)
    private BigDecimal electronicCollected;

    @Column(name = "other_collected", precision = 12, scale = 2)
    private BigDecimal otherCollected;

    @Column(name = "total_collected", precision = 12, scale = 2)
    private BigDecimal totalCollected;

    @Column(name = "vehicles_served")
    private Integer vehiclesServed;

    @Column(name = "expected_cash", precision = 12, scale = 2)
    private BigDecimal expectedCash;

    @Column(name = "reported_cash", precision = 12, scale = 2)
    private BigDecimal reportedCash;

    @Column(precision = 12, scale = 2)
    private BigDecimal difference;

    @Column(name = "other_income_collected", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherIncomeCollected;

    @Column(name = "expenses_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal expensesTotal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
