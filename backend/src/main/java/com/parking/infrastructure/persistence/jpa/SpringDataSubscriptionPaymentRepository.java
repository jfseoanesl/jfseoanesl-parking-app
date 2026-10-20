package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.SubscriptionPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SpringDataSubscriptionPaymentRepository extends JpaRepository<SubscriptionPaymentEntity, String> {

    List<SubscriptionPaymentEntity> findBySubscriptionIdOrderByPaidAtDesc(String subscriptionId);

    List<SubscriptionPaymentEntity> findByCashSessionId(String cashSessionId);

    List<SubscriptionPaymentEntity> findByPaidAtBetween(Instant from, Instant to);
}
