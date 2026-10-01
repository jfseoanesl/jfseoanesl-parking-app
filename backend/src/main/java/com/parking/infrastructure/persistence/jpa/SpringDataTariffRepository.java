package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.TariffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataTariffRepository extends JpaRepository<TariffEntity, String> {

    /**
     * Consulta nativa (SQL estandar, sin sintaxis especifica de un motor) en vez de JPQL: el
     * binding JPQL de un parametro LocalDate produce una clase de almacenamiento JDBC que no
     * coincide de forma fiable con el TEXT ISO-8601 que usa SQLite para columnas DATE, lo que
     * hacia que la comparacion nunca encontrara filas. Comparar como texto 'yyyy-MM-dd' es
     * inequivoco en SQLite y en MySQL (ver decisions/ADR-002-persistencia-sqlite.md).
     */
    @Query(value = "SELECT * FROM tariff WHERE vehicle_type_id = :vehicleTypeId AND active = 1 "
            + "AND valid_from <= :onDate AND (valid_to IS NULL OR valid_to >= :onDate)", nativeQuery = true)
    List<TariffEntity> findActiveForVehicleType(@Param("vehicleTypeId") String vehicleTypeId,
                                                 @Param("onDate") String onDate);

    // Metodos derivados simples (sin parametros de fecha) en vez de una consulta dinamica con
    // filtros nulos, para no reintroducir el tipo de incompatibilidad de binding ya encontrado
    // con este dialecto (ver decisions/ADR-005). Fase 7 (administracion de tarifas).
    List<TariffEntity> findByVehicleTypeId(String vehicleTypeId);

    List<TariffEntity> findByActive(boolean active);

    List<TariffEntity> findByVehicleTypeIdAndActive(String vehicleTypeId, boolean active);
}
