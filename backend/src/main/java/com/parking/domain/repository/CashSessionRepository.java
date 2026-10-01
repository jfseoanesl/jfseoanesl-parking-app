package com.parking.domain.repository;

import com.parking.domain.model.CashSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CashSessionRepository {

    CashSession save(CashSession cashSession);

    Optional<CashSession> findById(String id);

    /** BR-022: a lo sumo una caja abierta por empleado. */
    Optional<CashSession> findOpenByEmployeeId(String employeeId);

    /** RF-060 a RF-063: diferencias de caja del período (Fase 8). */
    List<CashSession> findClosedByClosedAtBetween(Instant from, Instant to);

    /** RF-064: cuántas cajas están abiertas ahora mismo (dashboard). */
    long countOpen();
}
