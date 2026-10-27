package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.CashTransaction;
import com.parking.domain.model.CashTransactionStatus;
import com.parking.domain.model.CashTransactionType;
import com.parking.domain.repository.CashTransactionRepository;
import com.parking.infrastructure.persistence.entity.CashTransactionEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataCashTransactionRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class CashTransactionRepositoryAdapter implements CashTransactionRepository {

    private final SpringDataCashTransactionRepository repository;

    public CashTransactionRepositoryAdapter(SpringDataCashTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public CashTransaction save(CashTransaction t) {
        return toDomain(repository.save(new CashTransactionEntity(t.id(), t.cashSessionId(), t.type().name(),
                t.conceptId(), t.description(), t.amount(), t.paymentMethodId(), t.operationNumber(), t.userId(),
                t.createdAt(), t.status().name(), t.voidReason(), t.voidedBy(), t.voidedAt())));
    }

    @Override
    public Optional<CashTransaction> findById(String id) {
        return repository.findById(id).map(CashTransactionRepositoryAdapter::toDomain);
    }

    @Override
    public List<CashTransaction> findByCashSessionId(String cashSessionId) {
        return repository.findByCashSessionId(cashSessionId).stream()
                .map(CashTransactionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<CashTransaction> findByCreatedAtBetween(Instant from, Instant to) {
        return repository.findByCreatedAtBetween(from, to).stream()
                .map(CashTransactionRepositoryAdapter::toDomain).toList();
    }

    private static CashTransaction toDomain(CashTransactionEntity e) {
        return new CashTransaction(e.getId(), e.getCashSessionId(), CashTransactionType.valueOf(e.getType()),
                e.getConceptId(), e.getDescription(), e.getAmount(), e.getPaymentMethodId(), e.getOperationNumber(),
                e.getUserId(), e.getCreatedAt(), CashTransactionStatus.valueOf(e.getStatus()), e.getVoidReason(),
                e.getVoidedBy(), e.getVoidedAt());
    }
}
