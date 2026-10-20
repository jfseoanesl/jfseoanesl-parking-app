package com.parking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_vehicle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionVehicleEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "subscription_id", nullable = false, length = 36)
    private String subscriptionId;

    @Column(nullable = false, length = 20)
    private String plate;

    @Column(name = "vehicle_type_id", nullable = false, length = 36)
    private String vehicleTypeId;
}
