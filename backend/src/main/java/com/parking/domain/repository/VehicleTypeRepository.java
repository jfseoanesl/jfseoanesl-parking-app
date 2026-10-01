package com.parking.domain.repository;

import com.parking.domain.model.VehicleType;

import java.util.List;
import java.util.Optional;

public interface VehicleTypeRepository {

    Optional<VehicleType> findById(String id);

    List<VehicleType> findAllActive();

    boolean existsByCode(String code);

    VehicleType save(VehicleType vehicleType);
}
