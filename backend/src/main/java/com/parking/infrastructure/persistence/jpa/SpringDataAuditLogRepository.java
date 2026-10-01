package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogEntity, String> {

    List<AuditLogEntity> findAllByOrderByOccurredAtDesc();

    List<AuditLogEntity> findByEntityNameOrderByOccurredAtDesc(String entityName);

    List<AuditLogEntity> findByEntityNameAndEntityIdOrderByOccurredAtDesc(String entityName, String entityId);
}
