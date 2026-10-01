package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.Payment;
import com.parking.domain.repository.PaymentRepository;
import com.parking.infrastructure.persistence.entity.PaymentEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataPaymentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private static final String PAYMENT_COUNTER = "PAYMENT";

    private final SpringDataPaymentRepository springDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public PaymentRepositoryAdapter(SpringDataPaymentRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Payment save(Payment payment) {
        return toDomain(springDataRepository.save(toEntity(payment)));
    }

    @Override
    public Optional<Payment> findById(String id) {
        return springDataRepository.findById(id).map(PaymentRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Payment> findByParkingSessionId(String parkingSessionId) {
        return springDataRepository.findByParkingSessionId(parkingSessionId).map(PaymentRepositoryAdapter::toDomain);
    }

    @Override
    public java.util.List<Payment> findByCashSessionId(String cashSessionId) {
        return springDataRepository.findByCashSessionId(cashSessionId).stream()
                .map(PaymentRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public long countByCashSessionId(String cashSessionId) {
        return springDataRepository.countByCashSessionId(cashSessionId);
    }

    /**
     * Reserva el siguiente consecutivo de forma atomica y portable (V5__operation_counter): el UPDATE
     * incrementa en la base, no en memoria. En MySQL bloquea la fila del contador hasta el commit, asi
     * que un segundo cobro simultaneo espera y obtiene el numero siguiente; en SQLite las transacciones
     * de escritura ya van en serie (transaction_mode=IMMEDIATE). La transaccion ve su propio UPDATE.
     */
    @Override
    public long nextOperationSequence() {
        int updated = entityManager
                .createNativeQuery("UPDATE operation_counter SET counter_value = counter_value + 1 WHERE name = :name")
                .setParameter("name", PAYMENT_COUNTER)
                .executeUpdate();
        if (updated != 1) {
            throw new IllegalStateException("Falta el contador de operaciones '" + PAYMENT_COUNTER
                    + "' (migracion V5__operation_counter)");
        }
        Number value = (Number) entityManager
                .createNativeQuery("SELECT counter_value FROM operation_counter WHERE name = :name")
                .setParameter("name", PAYMENT_COUNTER)
                .getSingleResult();
        return value.longValue();
    }

    @Override
    public java.util.List<Payment> findByPaidAtBetween(Instant from, Instant to) {
        return springDataRepository.findByPaidAtBetween(from, to).stream()
                .map(PaymentRepositoryAdapter::toDomain)
                .toList();
    }

    static Payment toDomain(PaymentEntity e) {
        return new Payment(e.getId(), e.getParkingSessionId(), e.getCashSessionId(), e.getCalculatedAmount(),
                e.getChargedAmount(), e.getPaymentMethodId(), e.getOperationNumber(), e.getUserId(),
                e.getPaidAt(), e.getCreatedAt());
    }

    static PaymentEntity toEntity(Payment p) {
        return new PaymentEntity(p.id(), p.parkingSessionId(), p.cashSessionId(), p.calculatedAmount(),
                p.chargedAmount(), p.paymentMethodId(), p.operationNumber(), p.userId(), p.paidAt(), p.createdAt());
    }
}
