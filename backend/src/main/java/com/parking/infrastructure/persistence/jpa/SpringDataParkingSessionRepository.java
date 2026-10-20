package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.ParkingSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpringDataParkingSessionRepository extends JpaRepository<ParkingSessionEntity, String> {

    Optional<ParkingSessionEntity> findByPlateAndStatus(String plate, String status);

    List<ParkingSessionEntity> findByStatus(String status);

    Optional<ParkingSessionEntity> findFirstBySubscriptionIdAndStatus(String subscriptionId, String status);

    List<ParkingSessionEntity> findByStatusAndPlateContainingIgnoreCase(String status, String plateFilter);

    // RF-060 a RF-065 (Fase 8). Solo usa parametros Instant (TIMESTAMP) - probadamente fiables
    // con este driver, a diferencia de LocalDate/LocalTime (ver decisions/ADR-005).
    List<ParkingSessionEntity> findByEntryDateTimeBetween(Instant from, Instant to);

    List<ParkingSessionEntity> findByStatusAndExitDateTimeBetween(String status, Instant from, Instant to);

    List<ParkingSessionEntity> findByStatusAndCancelledAtBetween(String status, Instant from, Instant to);

    List<ParkingSessionEntity> findTop10ByOrderByUpdatedAtDesc();
}
