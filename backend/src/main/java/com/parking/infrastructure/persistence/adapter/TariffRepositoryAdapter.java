package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.Tariff;
import com.parking.domain.model.TariffModality;
import com.parking.domain.model.TariffRounding;
import com.parking.domain.model.TariffTimeUnit;
import com.parking.domain.repository.TariffRepository;
import com.parking.infrastructure.persistence.entity.TariffEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataTariffRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class TariffRepositoryAdapter implements TariffRepository {

    private final SpringDataTariffRepository springDataRepository;

    public TariffRepositoryAdapter(SpringDataTariffRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public java.util.Optional<Tariff> findById(String id) {
        return springDataRepository.findById(id).map(TariffRepositoryAdapter::toDomain);
    }

    @Override
    public List<Tariff> findActiveForVehicleType(String vehicleTypeId, LocalDate onDate) {
        return springDataRepository.findActiveForVehicleType(vehicleTypeId, onDate.toString()).stream()
                .map(TariffRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Tariff> findAll(String vehicleTypeId, Boolean active) {
        List<TariffEntity> entities;
        if (vehicleTypeId != null && active != null) {
            entities = springDataRepository.findByVehicleTypeIdAndActive(vehicleTypeId, active);
        } else if (vehicleTypeId != null) {
            entities = springDataRepository.findByVehicleTypeId(vehicleTypeId);
        } else if (active != null) {
            entities = springDataRepository.findByActive(active);
        } else {
            entities = springDataRepository.findAll();
        }
        return entities.stream().map(TariffRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Tariff save(Tariff tariff) {
        return toDomain(springDataRepository.save(toEntity(tariff)));
    }

    static Tariff toDomain(TariffEntity e) {
        return new Tariff(
                e.getId(),
                e.getName(),
                e.getDescription(),
                e.getVehicleTypeId(),
                TariffModality.valueOf(e.getModality()),
                e.getValue(),
                e.getFirstFractionValue(),
                e.getStartTime() != null ? LocalTime.parse(e.getStartTime()) : null,
                e.getEndTime() != null ? LocalTime.parse(e.getEndTime()) : null,
                e.getToleranceMinutes(),
                e.getMinTimeMinutes(),
                e.getTimeUnit() != null ? TariffTimeUnit.valueOf(e.getTimeUnit()) : null,
                e.getFractionMinutes(),
                TariffRounding.valueOf(e.getRounding()),
                e.getPriority(),
                e.isActive(),
                LocalDate.parse(e.getValidFrom()),
                e.getValidTo() != null ? LocalDate.parse(e.getValidTo()) : null,
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getCreatedBy(),
                e.getUpdatedBy()
        );
    }

    static TariffEntity toEntity(Tariff t) {
        return new TariffEntity(
                t.id(),
                t.name(),
                t.description(),
                t.vehicleTypeId(),
                t.modality().name(),
                t.value(),
                t.firstFractionValue(),
                t.startTime() != null ? t.startTime().toString() : null,
                t.endTime() != null ? t.endTime().toString() : null,
                t.toleranceMinutes(),
                t.minTimeMinutes(),
                t.timeUnit() != null ? t.timeUnit().name() : null,
                t.fractionMinutes(),
                t.rounding().name(),
                t.priority(),
                t.active(),
                t.validFrom().toString(),
                t.validTo() != null ? t.validTo().toString() : null,
                t.createdAt(),
                t.updatedAt(),
                t.createdBy(),
                t.updatedBy()
        );
    }
}
