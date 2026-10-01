package com.parking.domain.repository;

import com.parking.domain.model.PaymentMethod;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository {

    Optional<PaymentMethod> findById(String id);

    List<PaymentMethod> findAllActive();

    boolean existsByCode(String code);

    PaymentMethod save(PaymentMethod paymentMethod);
}
