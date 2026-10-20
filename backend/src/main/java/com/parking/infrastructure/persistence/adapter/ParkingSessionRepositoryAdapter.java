package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.ParkingStatus;
import com.parking.domain.repository.ParkingSessionRepository;
import com.parking.infrastructure.persistence.entity.ParkingSessionEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataParkingSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class ParkingSessionRepositoryAdapter implements ParkingSessionRepository {

    private static final String ENTRY_COUNTER = "ENTRY";

    private final SpringDataParkingSessionRepository springDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ParkingSessionRepositoryAdapter(SpringDataParkingSessionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public ParkingSession save(ParkingSession session) {
        return toDomain(springDataRepository.save(toEntity(session)));
    }

    /** BR-027: mismo patron atomico que PaymentRepositoryAdapter#nextOperationSequence (V6). */
    @Override
    public long nextEntrySequence() {
        int updated = entityManager
                .createNativeQuery("UPDATE operation_counter SET counter_value = counter_value + 1 WHERE name = :name")
                .setParameter("name", ENTRY_COUNTER)
                .executeUpdate();
        if (updated != 1) {
            throw new IllegalStateException("Falta el contador '" + ENTRY_COUNTER
                    + "' (migracion V6__parking_session_entry_number)");
        }
        Number value = (Number) entityManager
                .createNativeQuery("SELECT counter_value FROM operation_counter WHERE name = :name")
                .setParameter("name", ENTRY_COUNTER)
                .getSingleResult();
        return value.longValue();
    }

    @Override
    public Optional<ParkingSession> findActiveBySubscriptionId(String subscriptionId) {
        return springDataRepository.findFirstBySubscriptionIdAndStatus(subscriptionId, ParkingStatus.EN_PARQUEADERO.name())
                .map(ParkingSessionRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<ParkingSession> findById(String id) {
        return springDataRepository.findById(id).map(ParkingSessionRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<ParkingSession> findActiveByPlate(String normalizedPlate) {
        return springDataRepository.findByPlateAndStatus(normalizedPlate, ParkingStatus.EN_PARQUEADERO.name())
                .map(ParkingSessionRepositoryAdapter::toDomain);
    }

    @Override
    public List<ParkingSession> findAllActive(String plateFilter) {
        String status = ParkingStatus.EN_PARQUEADERO.name();
        List<ParkingSessionEntity> entities = (plateFilter == null || plateFilter.isBlank())
                ? springDataRepository.findByStatus(status)
                : springDataRepository.findByStatusAndPlateContainingIgnoreCase(status, plateFilter);
        return entities.stream().map(ParkingSessionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<ParkingSession> findByEntryDateTimeBetween(Instant from, Instant to) {
        return springDataRepository.findByEntryDateTimeBetween(from, to).stream()
                .map(ParkingSessionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<ParkingSession> findFinalizedByExitDateTimeBetween(Instant from, Instant to) {
        return springDataRepository
                .findByStatusAndExitDateTimeBetween(ParkingStatus.FINALIZADO.name(), from, to).stream()
                .map(ParkingSessionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<ParkingSession> findCancelledByCancelledAtBetween(Instant from, Instant to) {
        return springDataRepository
                .findByStatusAndCancelledAtBetween(ParkingStatus.ANULADO.name(), from, to).stream()
                .map(ParkingSessionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<ParkingSession> findRecent() {
        return springDataRepository.findTop10ByOrderByUpdatedAtDesc().stream()
                .map(ParkingSessionRepositoryAdapter::toDomain).toList();
    }

    static ParkingSession toDomain(ParkingSessionEntity e) {
        return new ParkingSession(
                e.getId(), e.getPlate(), e.getVehicleTypeId(), e.getEntryDateTime(), e.getExitDateTime(),
                ParkingStatus.valueOf(e.getStatus()), e.getEntryEmployeeId(), e.getExitEmployeeId(),
                e.getAppliedTariffId(), e.getCalculatedAmount(), e.getNotes(), e.getCancelReason(),
                e.getCancelledBy(), e.getCancelledAt(), e.getCreatedAt(), e.getUpdatedAt(), e.getEntryNumber(),
                e.getSubscriptionId());
    }

    static ParkingSessionEntity toEntity(ParkingSession s) {
        return new ParkingSessionEntity(
                s.id(), s.plate(), s.vehicleTypeId(), s.entryDateTime(), s.exitDateTime(),
                s.status().name(), s.entryEmployeeId(), s.exitEmployeeId(), s.appliedTariffId(),
                s.calculatedAmount(), s.notes(), s.cancelReason(), s.cancelledBy(), s.cancelledAt(),
                s.createdAt(), s.updatedAt(), s.entryNumber(), s.subscriptionId());
    }
}
