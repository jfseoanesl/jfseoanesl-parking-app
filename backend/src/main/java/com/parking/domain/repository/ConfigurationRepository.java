package com.parking.domain.repository;

import com.parking.domain.model.Configuration;

import java.util.Optional;

public interface ConfigurationRepository {

    /** Configuration.java §03-domain-model.md: fila unica por instalacion. */
    Optional<Configuration> find();

    Configuration save(Configuration configuration);
}
