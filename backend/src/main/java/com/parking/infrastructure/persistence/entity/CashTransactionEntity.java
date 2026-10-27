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

@Entity
@Table(name = "cash_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashTransactionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "cash_session_id", nullable = false, length = 36)
    private String cashSessionId;

    @Column(nullable = false, length = 10)
    private String type;

    @Column(name = "concept_id", nullable = false, length = 36)
    private String conceptId;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method_id", nullable = false, length = 36)
    private String paymentMethodId;

    @Column(name = "operation_number", nullable = false, length = 30)
    private String operationNumber;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "void_reason", length = 255)
    private String voidReason;

    @Column(name = "voided_by", length = 36)
    private String voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;
}
