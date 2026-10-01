package com.parking.presentation.dto;

import com.parking.domain.model.VehicleType;

public record VehicleTypeResponse(String id, String code, String name) {

    public static VehicleTypeResponse from(VehicleType vehicleType) {
        return new VehicleTypeResponse(vehicleType.id(), vehicleType.code(), vehicleType.name());
    }
}
