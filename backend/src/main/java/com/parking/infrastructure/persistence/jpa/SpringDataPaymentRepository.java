package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, String> {

    Optional<PaymentEntity> findByParkingSessionId(String parkingSessionId);

    List<PaymentEntity> findByCashSessionId(String cashSessionId);

    long countByCashSessionId(String cashSessionId);

    List<PaymentEntity> findByPaidAtBetween(Instant from, Instant to);
}
