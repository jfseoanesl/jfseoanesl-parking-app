package com.parking.domain.repository;

import com.parking.domain.model.AuditLog;

import java.util.List;

/** BR-025, RF-067. */
public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    /** Cualquier filtro puede ser null (sin filtrar por ese campo). Orden: más reciente primero. */
    List<AuditLog> findAll(String entityName, String entityId);

    /**
     * RF-067: busqueda con filtros opcionales (null = sin filtro). {@code from}/{@code to} acotan
     * occurredAt (inclusivo). Mas recientes primero.
     */
    List<AuditLog> search(String entityName, String entityId, String userId, java.time.Instant from,
                          java.time.Instant to);
}
