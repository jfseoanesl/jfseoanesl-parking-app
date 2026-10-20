package com.parking.application.parking;

import com.parking.domain.exception.BusinessRuleViolationException;
import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.ParkingStatus;
import com.parking.domain.model.PlateNormalizer;
import com.parking.domain.model.Subscription;
import com.parking.domain.model.VehicleType;
import com.parking.application.subscription.SubscriptionService;
import com.parking.domain.repository.ParkingSessionRepository;
import com.parking.domain.repository.VehicleTypeRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** UC-002 (RF-023 a RF-029). */
@Service
public class ParkingEntryService {

    private final ParkingSessionRepository parkingSessionRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final TimeProvider timeProvider;
    private final SubscriptionService subscriptionService;

    /** {@code subscriptionNotice}: aviso para el empleado cuando la placa es de una mensualidad (BR-029). */
    public record EntryResult(ParkingSession session, String subscriptionNotice) {
    }

    public ParkingEntryService(ParkingSessionRepository parkingSessionRepository,
                                VehicleTypeRepository vehicleTypeRepository, TimeProvider timeProvider,
                                SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
        this.parkingSessionRepository = parkingSessionRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public EntryResult registerEntry(String rawPlate, String vehicleTypeId, String notes, String employeeId) {
        String plate = PlateNormalizer.normalize(rawPlate); // BR-002

        VehicleType vehicleType = vehicleTypeRepository.findById(vehicleTypeId)
                .filter(VehicleType::active)
                .orElseThrow(() -> new NotFoundException("Tipo de vehículo no encontrado o inactivo"));

        // BR-001: la barrera definitiva es el indice unico condicional en BD; esta verificacion
        // da una respuesta rapida y un mensaje claro antes de llegar a la base de datos.
        parkingSessionRepository.findActiveByPlate(plate).ifPresent(existing -> {
            throw new BusinessRuleViolationException("DUPLICATE_ACTIVE_PLATE",
                    "Ya existe un parqueo activo para la placa " + plate);
        });

        // BR-029: una placa autorizada se vincula a su mensualidad, salvo que la mensualidad ya tenga
        // otro vehiculo dentro (solo uno a la vez): entonces ingresa como cliente normal, con aviso.
        String subscriptionId = null;
        String notice = null;
        Subscription subscription = subscriptionService.findActiveByPlate(plate).orElse(null);
        if (subscription != null) {
            ParkingSession inside = parkingSessionRepository.findActiveBySubscriptionId(subscription.id()).orElse(null);
            if (inside == null) {
                subscriptionId = subscription.id();
                notice = "Cliente mensual: " + subscription.customerName();
            } else {
                notice = "La mensualidad de " + subscription.customerName() + " ya tiene el vehículo "
                        + inside.plate() + " dentro; este ingreso se cobrará como cliente normal";
            }
        }

        Instant now = timeProvider.now();
        ParkingSession session = new ParkingSession(
                UUID.randomUUID().toString(), plate, vehicleType.id(), now, null,
                ParkingStatus.EN_PARQUEADERO, employeeId, null, null, null,
                notes, null, null, null, now, now,
                "ING-" + String.format("%06d", parkingSessionRepository.nextEntrySequence()), // BR-027
                subscriptionId);

        return new EntryResult(parkingSessionRepository.save(session), notice);
    }
}
