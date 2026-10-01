package com.parking.domain.repository;

import com.parking.domain.model.Tariff;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TariffRepository {

    Optional<Tariff> findById(String id);

    /** Tarifas activas y vigentes para un tipo de vehículo en la fecha dada (BR-009). */
    List<Tariff> findActiveForVehicleType(String vehicleTypeId, LocalDate onDate);

    /** RF-018/RF-019/RF-020: administración (Fase 7). */
    List<Tariff> findAll(String vehicleTypeId, Boolean active);

    Tariff save(Tariff tariff);
}
