package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.VehicleTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataVehicleTypeRepository extends JpaRepository<VehicleTypeEntity, String> {

    List<VehicleTypeEntity> findByActiveTrue();

    boolean existsByCode(String code);
}
