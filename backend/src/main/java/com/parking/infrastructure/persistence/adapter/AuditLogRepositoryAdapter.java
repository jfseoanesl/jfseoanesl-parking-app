package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.AuditLog;
import com.parking.domain.repository.AuditLogRepository;
import com.parking.infrastructure.persistence.entity.AuditLogEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataAuditLogRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final SpringDataAuditLogRepository springDataRepository;

    public AuditLogRepositoryAdapter(SpringDataAuditLogRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogEntity entity = new AuditLogEntity(auditLog.id(), auditLog.userId(), auditLog.action(),
                auditLog.entityName(), auditLog.entityId(), auditLog.oldValue(), auditLog.newValue(),
                auditLog.occurredAt());
        AuditLogEntity saved = springDataRepository.save(entity);
        return new AuditLog(saved.getId(), saved.getUserId(), saved.getAction(), saved.getEntityName(),
                saved.getEntityId(), saved.getOldValue(), saved.getNewValue(), saved.getOccurredAt());
    }

    @Override
    public List<AuditLog> findAll(String entityName, String entityId) {
        List<AuditLogEntity> entities;
        if (entityName != null && entityId != null) {
            entities = springDataRepository.findByEntityNameAndEntityIdOrderByOccurredAtDesc(entityName, entityId);
        } else if (entityName != null) {
            entities = springDataRepository.findByEntityNameOrderByOccurredAtDesc(entityName);
        } else {
            entities = springDataRepository.findAllByOrderByOccurredAtDesc();
        }
        return entities.stream().map(AuditLogRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<AuditLog> search(String entityName, String entityId, String userId, java.time.Instant from,
                                 java.time.Instant to) {
        // Filtros adicionales en memoria sobre la consulta por entidad: volumen bajo (auditoria de un
        // parqueadero) y evita consultas dinamicas distintas por motor.
        return findAll(entityName, entityId).stream()
                .filter(a -> userId == null || userId.equals(a.userId()))
                .filter(a -> from == null || !a.occurredAt().isBefore(from))
                .filter(a -> to == null || !a.occurredAt().isAfter(to))
                .toList();
    }

    static AuditLog toDomain(AuditLogEntity e) {
        return new AuditLog(e.getId(), e.getUserId(), e.getAction(), e.getEntityName(), e.getEntityId(),
                e.getOldValue(), e.getNewValue(), e.getOccurredAt());
    }
}
