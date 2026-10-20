package com.parking.domain.repository;

import com.parking.domain.model.SubscriptionPayment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** RF-077: pagos de mensualidades cobrados en caja. */
public interface SubscriptionPaymentRepository {

    SubscriptionPayment save(SubscriptionPayment payment);

    Optional<SubscriptionPayment> findById(String id);

    List<SubscriptionPayment> findBySubscriptionId(String subscriptionId);

    /** RF-055: suman en el arqueo de la caja donde se cobraron. */
    List<SubscriptionPayment> findByCashSessionId(String cashSessionId);

    /** RF-060: recaudo por rango de fechas. */
    List<SubscriptionPayment> findByPaidAtBetween(Instant from, Instant to);
}
