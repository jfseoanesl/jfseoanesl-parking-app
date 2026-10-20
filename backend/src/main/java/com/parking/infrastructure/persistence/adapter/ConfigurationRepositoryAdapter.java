package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.Configuration;
import com.parking.domain.repository.ConfigurationRepository;
import com.parking.infrastructure.persistence.entity.ConfigurationEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataConfigurationRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConfigurationRepositoryAdapter implements ConfigurationRepository {

    private final SpringDataConfigurationRepository springDataRepository;

    public ConfigurationRepositoryAdapter(SpringDataConfigurationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Configuration> find() {
        return springDataRepository.findAll().stream().findFirst().map(ConfigurationRepositoryAdapter::toDomain);
    }

    @Override
    public Configuration save(Configuration configuration) {
        ConfigurationEntity entity = new ConfigurationEntity(configuration.id(), configuration.parkingName(),
                configuration.nit(), configuration.address(), configuration.phone(), configuration.maxCapacity(),
                configuration.timezone(), configuration.currency(), configuration.receiptFormatJson(),
                configuration.updatedAt(), configuration.updatedBy(), configuration.subscriptionGraceDays());
        return toDomain(springDataRepository.save(entity));
    }

    static Configuration toDomain(ConfigurationEntity e) {
        return new Configuration(e.getId(), e.getParkingName(), e.getNit(), e.getAddress(), e.getPhone(),
                e.getMaxCapacity(), e.getTimezone(), e.getCurrency(), e.getReceiptFormatJson(), e.getUpdatedAt(),
                e.getUpdatedBy(), e.getSubscriptionGraceDays());
    }
}
