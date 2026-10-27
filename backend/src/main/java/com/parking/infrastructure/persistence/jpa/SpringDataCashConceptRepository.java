package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.CashConceptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCashConceptRepository extends JpaRepository<CashConceptEntity, String> {
}
