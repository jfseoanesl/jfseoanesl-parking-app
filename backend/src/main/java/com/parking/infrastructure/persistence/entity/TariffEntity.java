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
 * start_time/end_time/valid_from/valid_to se mapean como String (no LocalTime/LocalDate): el
 * dialecto community de Hibernate para SQLite (7.4.5.Final) no logra releer columnas DATE/TIME
 * con el driver xerial sqlite-jdbc (su getTimestamp()/getDate() exige el formato estricto
 * "yyyy-MM-dd HH:mm:ss.SSS" incluso para un valor solo-fecha como "2026-01-01", y lanza
 * ParseException). Los TIMESTAMP (Instant) no tienen este problema. La conversion a
 * LocalDate/LocalTime del dominio ocurre en TariffRepositoryAdapter. Ver decisions/ADR-005.
 */
@Entity
@Table(name = "tariff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TariffEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "vehicle_type_id", nullable = false, length = 36)
    private String vehicleTypeId;

    @Column(nullable = false, length = 20)
    private String modality;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "start_time")
    private String startTime;

    @Column(name = "end_time")
    private String endTime;

    @Column(name = "tolerance_minutes")
    private Integer toleranceMinutes;

    @Column(name = "min_time_minutes")
    private Integer minTimeMinutes;

    @Column(name = "time_unit", length = 10)
    private String timeUnit;

    @Column(name = "fraction_minutes")
    private Integer fractionMinutes;

    @Column(nullable = false, length = 10)
    private String rounding;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "valid_from", nullable = false)
    private String validFrom;

    @Column(name = "valid_to")
    private String validTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}
