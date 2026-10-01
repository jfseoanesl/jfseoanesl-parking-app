package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.CashMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCashMovementRepository extends JpaRepository<CashMovementEntity, String> {
}
