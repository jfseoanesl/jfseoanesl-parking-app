package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.VehicleType;
import com.parking.domain.repository.VehicleTypeRepository;
import com.parking.infrastructure.persistence.entity.VehicleTypeEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataVehicleTypeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class VehicleTypeRepositoryAdapter implements VehicleTypeRepository {

    private final SpringDataVehicleTypeRepository springDataRepository;

    public VehicleTypeRepositoryAdapter(SpringDataVehicleTypeRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<VehicleType> findById(String id) {
        return springDataRepository.findById(id).map(VehicleTypeRepositoryAdapter::toDomain);
    }

    @Override
    public List<VehicleType> findAllActive() {
        return springDataRepository.findByActiveTrue().stream()
                .map(VehicleTypeRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return springDataRepository.existsByCode(code);
    }

    @Override
    public VehicleType save(VehicleType vehicleType) {
        VehicleTypeEntity entity = new VehicleTypeEntity(vehicleType.id(), vehicleType.code(), vehicleType.name(),
                vehicleType.active());
        return toDomain(springDataRepository.save(entity));
    }

    static VehicleType toDomain(VehicleTypeEntity entity) {
        return new VehicleType(entity.getId(), entity.getCode(), entity.getName(), entity.isActive());
    }
}
