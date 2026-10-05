package com.parking.domain.repository;

import com.parking.domain.model.ParkingSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository {

    ParkingSession save(ParkingSession session);

    Optional<ParkingSession> findById(String id);

    /** BR-027: reserva atómica del siguiente consecutivo de ingreso. */
    long nextEntrySequence();

    /** BR-001: a lo sumo una coincide, pero se expone como Optional por claridad de intención. */
    Optional<ParkingSession> findActiveByPlate(String normalizedPlate);

    List<ParkingSession> findAllActive(String plateFilter);

    /** RF-060 a RF-065: soporte de reportes y dashboard (Fase 8). */
    List<ParkingSession> findByEntryDateTimeBetween(Instant from, Instant to);

    List<ParkingSession> findFinalizedByExitDateTimeBetween(Instant from, Instant to);

    List<ParkingSession> findCancelledByCancelledAtBetween(Instant from, Instant to);

    /** RF-064: últimos movimientos del dashboard (10 más recientes por fecha de actualización). */
    List<ParkingSession> findRecent();
}
