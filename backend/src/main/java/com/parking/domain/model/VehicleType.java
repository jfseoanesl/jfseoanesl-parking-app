package com.parking.domain.model;

/** Ver 03-domain-model.md §VehicleType. Semilla: CAR, MOTORCYCLE (RF-016). */
public record VehicleType(String id, String code, String name, boolean active) {
}
