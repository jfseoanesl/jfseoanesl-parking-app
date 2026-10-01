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
@Table(name = "cash_movement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashMovementEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "cash_session_id", nullable = false, length = 36)
    private String cashSessionId;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
