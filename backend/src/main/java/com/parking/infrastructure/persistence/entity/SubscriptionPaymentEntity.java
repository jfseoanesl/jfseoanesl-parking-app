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

/** period_from/period_to como String (yyyy-MM-dd), ver ADR-005. */
@Entity
@Table(name = "subscription_payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPaymentEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "subscription_id", nullable = false, length = 36)
    private String subscriptionId;

    @Column(name = "cash_session_id", nullable = false, length = 36)
    private String cashSessionId;

    @Column(name = "payment_method_id", nullable = false, length = 36)
    private String paymentMethodId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "period_from", nullable = false)
    private String periodFrom;

    @Column(name = "period_to", nullable = false)
    private String periodTo;

    @Column(name = "operation_number", nullable = false, length = 30)
    private String operationNumber;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;
}
