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

/** paid_until se mapea como String (yyyy-MM-dd), igual que las fechas de TariffEntity (ADR-005). */
@Entity
@Table(name = "subscription")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "document_id", length = 30)
    private String documentId;

    @Column(length = 30)
    private String phone;

    @Column(name = "monthly_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyFee;

    @Column(name = "cut_off_day", nullable = false)
    private int cutOffDay;

    @Column(name = "paid_until")
    private String paidUntil;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;
}
