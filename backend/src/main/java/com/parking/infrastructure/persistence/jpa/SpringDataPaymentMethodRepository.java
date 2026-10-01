package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.PaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataPaymentMethodRepository extends JpaRepository<PaymentMethodEntity, String> {

    List<PaymentMethodEntity> findByActiveTrue();

    boolean existsByCode(String code);
}
