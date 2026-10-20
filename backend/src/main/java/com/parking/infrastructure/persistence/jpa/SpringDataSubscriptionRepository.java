package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {
}
