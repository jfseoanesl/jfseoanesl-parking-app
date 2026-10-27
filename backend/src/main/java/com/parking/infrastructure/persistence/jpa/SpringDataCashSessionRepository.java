package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.CashSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpringDataCashSessionRepository extends JpaRepository<CashSessionEntity, String> {

    Optional<CashSessionEntity> findByEmployeeIdAndStatus(String employeeId, String status);

    List<CashSessionEntity> findByStatusAndClosedAtBetween(String status, Instant from, Instant to);

    long countByStatus(String status);

    List<CashSessionEntity> findByStatus(String status);
}
