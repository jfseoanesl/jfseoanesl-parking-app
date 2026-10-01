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

/**
 * La columna generada "plate_active" (07-data-model.md, BR-001) no se mapea aqui: es
 * calculada/mantenida por la base de datos, la aplicacion nunca la escribe ni la lee.
 */
@Entity
@Table(name = "parking_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSessionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 20)
    private String plate;

    @Column(name = "vehicle_type_id", nullable = false, length = 36)
    private String vehicleTypeId;

    @Column(name = "entry_datetime", nullable = false)
    private Instant entryDateTime;

    @Column(name = "exit_datetime")
    private Instant exitDateTime;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "entry_employee_id", nullable = false, length = 36)
    private String entryEmployeeId;

    @Column(name = "exit_employee_id", length = 36)
    private String exitEmployeeId;

    @Column(name = "applied_tariff_id", length = 36)
    private String appliedTariffId;

    @Column(name = "calculated_amount", precision = 12, scale = 2)
    private BigDecimal calculatedAmount;

    @Column(length = 255)
    private String notes;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "cancelled_by", length = 36)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
