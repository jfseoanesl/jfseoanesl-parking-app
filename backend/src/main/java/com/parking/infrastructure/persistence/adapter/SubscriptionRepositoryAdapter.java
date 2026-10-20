package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.Subscription;
import com.parking.domain.model.SubscriptionVehicle;
import com.parking.domain.repository.SubscriptionRepository;
import com.parking.infrastructure.persistence.entity.SubscriptionEntity;
import com.parking.infrastructure.persistence.entity.SubscriptionVehicleEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataSubscriptionRepository;
import com.parking.infrastructure.persistence.jpa.SpringDataSubscriptionVehicleRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SubscriptionRepositoryAdapter implements SubscriptionRepository {

    private final SpringDataSubscriptionRepository subscriptions;
    private final SpringDataSubscriptionVehicleRepository vehicles;

    public SubscriptionRepositoryAdapter(SpringDataSubscriptionRepository subscriptions,
                                         SpringDataSubscriptionVehicleRepository vehicles) {
        this.subscriptions = subscriptions;
        this.vehicles = vehicles;
    }

    @Override
    public Subscription save(Subscription s) {
        SubscriptionEntity saved = subscriptions.saveAndFlush(new SubscriptionEntity(s.id(), s.customerName(),
                s.documentId(), s.phone(), s.monthlyFee(), s.cutOffDay(),
                s.paidUntil() != null ? s.paidUntil().toString() : null, s.active(), s.notes(), s.createdAt(),
                s.updatedAt(), s.createdBy()));
        if (s.vehicles() != null) {
            // Se borran primero (con flush) para que una placa pueda conservarse o moverse sin chocar
            // con el indice unico uk_subscription_vehicle_plate.
            vehicles.deleteBySubscriptionId(s.id());
            vehicles.saveAllAndFlush(s.vehicles().stream()
                    .map(v -> new SubscriptionVehicleEntity(v.id(), s.id(), v.plate(), v.vehicleTypeId()))
                    .toList());
        }
        return toDomain(saved, vehiclesOf(s.id()));
    }

    @Override
    public Optional<Subscription> findById(String id) {
        return subscriptions.findById(id).map(e -> toDomain(e, vehiclesOf(id)));
    }

    @Override
    public List<Subscription> findAll() {
        Map<String, List<SubscriptionVehicle>> bySubscription = vehicles.findAll().stream()
                .map(SubscriptionRepositoryAdapter::toDomain)
                .collect(Collectors.groupingBy(SubscriptionVehicle::subscriptionId));
        return subscriptions.findAll().stream()
                .map(e -> toDomain(e, bySubscription.getOrDefault(e.getId(), List.of())))
                .sorted(Comparator.comparing(Subscription::customerName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public Optional<SubscriptionVehicle> findVehicleByPlate(String normalizedPlate) {
        return vehicles.findByPlate(normalizedPlate).map(SubscriptionRepositoryAdapter::toDomain);
    }

    private List<SubscriptionVehicle> vehiclesOf(String subscriptionId) {
        return vehicles.findBySubscriptionId(subscriptionId).stream()
                .map(SubscriptionRepositoryAdapter::toDomain)
                .sorted(Comparator.comparing(SubscriptionVehicle::plate))
                .toList();
    }

    private static Subscription toDomain(SubscriptionEntity e, List<SubscriptionVehicle> vehicles) {
        return new Subscription(e.getId(), e.getCustomerName(), e.getDocumentId(), e.getPhone(), e.getMonthlyFee(),
                e.getCutOffDay(), e.getPaidUntil() != null ? LocalDate.parse(e.getPaidUntil()) : null, e.isActive(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt(), e.getCreatedBy(), vehicles);
    }

    private static SubscriptionVehicle toDomain(SubscriptionVehicleEntity e) {
        return new SubscriptionVehicle(e.getId(), e.getSubscriptionId(), e.getPlate(), e.getVehicleTypeId());
    }
}
