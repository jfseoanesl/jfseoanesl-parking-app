package com.parking.domain.repository;

import com.parking.domain.model.Subscription;
import com.parking.domain.model.SubscriptionVehicle;

import java.util.List;
import java.util.Optional;

/** RF-074: mensualidades con sus placas autorizadas. */
public interface SubscriptionRepository {

    /** Guarda la mensualidad y reemplaza su lista de placas por {@code subscription.vehicles()}. */
    Subscription save(Subscription subscription);

    Optional<Subscription> findById(String id);

    List<Subscription> findAll();

    /** BR-029: placa normalizada → su placa autorizada, si existe. */
    Optional<SubscriptionVehicle> findVehicleByPlate(String normalizedPlate);
}
