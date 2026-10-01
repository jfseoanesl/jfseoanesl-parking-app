package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.ConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataConfigurationRepository extends JpaRepository<ConfigurationEntity, String> {
}
