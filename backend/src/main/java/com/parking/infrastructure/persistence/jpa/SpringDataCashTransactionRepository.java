package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.CashTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SpringDataCashTransactionRepository extends JpaRepository<CashTransactionEntity, String> {

    List<CashTransactionEntity> findByCashSessionId(String cashSessionId);

    List<CashTransactionEntity> findByCreatedAtBetween(Instant from, Instant to);
}
