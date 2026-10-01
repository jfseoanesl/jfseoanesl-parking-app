package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.CashMovement;
import com.parking.domain.model.CashMovementType;
import com.parking.domain.repository.CashMovementRepository;
import com.parking.infrastructure.persistence.entity.CashMovementEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataCashMovementRepository;
import org.springframework.stereotype.Component;

@Component
public class CashMovementRepositoryAdapter implements CashMovementRepository {

    private final SpringDataCashMovementRepository springDataRepository;

    public CashMovementRepositoryAdapter(SpringDataCashMovementRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public CashMovement save(CashMovement movement) {
        CashMovementEntity entity = new CashMovementEntity(movement.id(), movement.cashSessionId(),
                movement.paymentId(), movement.type().name(), movement.amount(), movement.description(),
                movement.createdAt());
        CashMovementEntity saved = springDataRepository.save(entity);
        return new CashMovement(saved.getId(), saved.getCashSessionId(), saved.getPaymentId(),
                CashMovementType.valueOf(saved.getType()), saved.getAmount(), saved.getDescription(),
                saved.getCreatedAt());
    }
}
