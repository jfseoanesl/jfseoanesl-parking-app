package com.parking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "parking_name", nullable = false, length = 150)
    private String parkingName;

    @Column(length = 30)
    private String nit;

    @Column(length = 255)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(nullable = false, length = 60)
    private String timezone;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "receipt_format_json")
    private String receiptFormatJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}
