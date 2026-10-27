package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.CashConcept;
import com.parking.domain.model.CashTransactionType;
import com.parking.domain.repository.CashConceptRepository;
import com.parking.infrastructure.persistence.entity.CashConceptEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataCashConceptRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CashConceptRepositoryAdapter implements CashConceptRepository {

    private final SpringDataCashConceptRepository repository;

    public CashConceptRepositoryAdapter(SpringDataCashConceptRepository repository) {
        this.repository = repository;
    }

    @Override
    public CashConcept save(CashConcept c) {
        return toDomain(repository.save(new CashConceptEntity(c.id(), c.type().name(), c.name(), c.active())));
    }

    @Override
    public Optional<CashConcept> findById(String id) {
        return repository.findById(id).map(CashConceptRepositoryAdapter::toDomain);
    }

    @Override
    public List<CashConcept> findAll() {
        return repository.findAll().stream().map(CashConceptRepositoryAdapter::toDomain).toList();
    }

    private static CashConcept toDomain(CashConceptEntity e) {
        return new CashConcept(e.getId(), CashTransactionType.valueOf(e.getType()), e.getName(), e.isActive());
    }
}
