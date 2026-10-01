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
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "parking_session_id", nullable = false, unique = true, length = 36)
    private String parkingSessionId;

    @Column(name = "cash_session_id", nullable = false, length = 36)
    private String cashSessionId;

    @Column(name = "calculated_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal calculatedAmount;

    @Column(name = "charged_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargedAmount;

    @Column(name = "payment_method_id", nullable = false, length = 36)
    private String paymentMethodId;

    @Column(name = "operation_number", nullable = false, unique = true, length = 30)
    private String operationNumber;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
