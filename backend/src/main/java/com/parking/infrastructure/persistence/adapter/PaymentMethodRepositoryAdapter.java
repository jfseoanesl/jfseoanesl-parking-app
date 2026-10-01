package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.PaymentMethod;
import com.parking.domain.model.PaymentMethodCategory;
import com.parking.domain.repository.PaymentMethodRepository;
import com.parking.infrastructure.persistence.entity.PaymentMethodEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataPaymentMethodRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PaymentMethodRepositoryAdapter implements PaymentMethodRepository {

    private final SpringDataPaymentMethodRepository springDataRepository;

    public PaymentMethodRepositoryAdapter(SpringDataPaymentMethodRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<PaymentMethod> findById(String id) {
        return springDataRepository.findById(id).map(PaymentMethodRepositoryAdapter::toDomain);
    }

    @Override
    public List<PaymentMethod> findAllActive() {
        return springDataRepository.findByActiveTrue().stream()
                .map(PaymentMethodRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return springDataRepository.existsByCode(code);
    }

    @Override
    public PaymentMethod save(PaymentMethod paymentMethod) {
        PaymentMethodEntity entity = new PaymentMethodEntity(paymentMethod.id(), paymentMethod.code(),
                paymentMethod.name(), paymentMethod.category().name(), paymentMethod.active());
        return toDomain(springDataRepository.save(entity));
    }

    static PaymentMethod toDomain(PaymentMethodEntity e) {
        return new PaymentMethod(
                e.getId(), e.getCode(), e.getName(), PaymentMethodCategory.valueOf(e.getCategory()), e.isActive());
    }
}
