package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.SubscriptionPayment;
import com.parking.domain.repository.SubscriptionPaymentRepository;
import com.parking.infrastructure.persistence.entity.SubscriptionPaymentEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataSubscriptionPaymentRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class SubscriptionPaymentRepositoryAdapter implements SubscriptionPaymentRepository {

    private final SpringDataSubscriptionPaymentRepository repository;

    public SubscriptionPaymentRepositoryAdapter(SpringDataSubscriptionPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public SubscriptionPayment save(SubscriptionPayment p) {
        return toDomain(repository.save(new SubscriptionPaymentEntity(p.id(), p.subscriptionId(), p.cashSessionId(),
                p.paymentMethodId(), p.userId(), p.amount(), p.periodFrom().toString(), p.periodTo().toString(),
                p.operationNumber(), p.paidAt())));
    }

    @Override
    public Optional<SubscriptionPayment> findById(String id) {
        return repository.findById(id).map(SubscriptionPaymentRepositoryAdapter::toDomain);
    }

    @Override
    public List<SubscriptionPayment> findBySubscriptionId(String subscriptionId) {
        return repository.findBySubscriptionIdOrderByPaidAtDesc(subscriptionId).stream()
                .map(SubscriptionPaymentRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<SubscriptionPayment> findByCashSessionId(String cashSessionId) {
        return repository.findByCashSessionId(cashSessionId).stream()
                .map(SubscriptionPaymentRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<SubscriptionPayment> findByPaidAtBetween(Instant from, Instant to) {
        return repository.findByPaidAtBetween(from, to).stream()
                .map(SubscriptionPaymentRepositoryAdapter::toDomain).toList();
    }

    private static SubscriptionPayment toDomain(SubscriptionPaymentEntity e) {
        return new SubscriptionPayment(e.getId(), e.getSubscriptionId(), e.getCashSessionId(), e.getPaymentMethodId(),
                e.getUserId(), e.getAmount(), LocalDate.parse(e.getPeriodFrom()), LocalDate.parse(e.getPeriodTo()),
                e.getOperationNumber(), e.getPaidAt());
    }
}
