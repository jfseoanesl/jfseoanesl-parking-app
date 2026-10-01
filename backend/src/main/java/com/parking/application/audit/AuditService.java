package com.parking.application.audit;

import com.parking.domain.model.AuditLog;
import com.parking.domain.repository.AuditLogRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * BR-025: registro de auditoría para operaciones sensibles. Fase 6 solo necesita escribir
 * (corrección administrativa de cierre de caja); la consulta (GET /api/audit-logs) y la
 * cobertura completa de "operaciones sensibles" del prompt (tarifas, anulaciones, usuarios)
 * llegan en Fase 7.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final TimeProvider timeProvider;

    public AuditService(AuditLogRepository auditLogRepository, TimeProvider timeProvider) {
        this.auditLogRepository = auditLogRepository;
        this.timeProvider = timeProvider;
    }

    public void record(String userId, String action, String entityName, String entityId,
                        String oldValue, String newValue) {
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID().toString(), userId, action, entityName, entityId,
                oldValue, newValue, timeProvider.now()));
    }
}
