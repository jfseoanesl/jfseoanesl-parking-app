package com.parking.domain.repository;

import com.parking.domain.model.Payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(String id);

    /** BR-021: a lo sumo un pago por sesion. */
    Optional<Payment> findByParkingSessionId(String parkingSessionId);

    /** RF-055: pagos de una caja, para calcular el arqueo. */
    List<Payment> findByCashSessionId(String cashSessionId);

    long countByCashSessionId(String cashSessionId);

    /**
     * Siguiente consecutivo de operacion (RF-049), unico aun con cobros simultaneos: se reserva de
     * forma atomica en la base de datos. Debe llamarse dentro de la transaccion del cobro.
     */
    long nextOperationSequence();

    /** RF-060 a RF-065: recaudo por rango de fechas (Fase 8). */
    List<Payment> findByPaidAtBetween(Instant from, Instant to);
}
