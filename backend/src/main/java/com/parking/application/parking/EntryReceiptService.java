package com.parking.application.parking;

import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.Configuration;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.ParkingStatus;
import com.parking.domain.model.User;
import com.parking.domain.model.VehicleType;
import com.parking.domain.repository.ConfigurationRepository;
import com.parking.domain.repository.ParkingSessionRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.domain.repository.VehicleTypeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * RF-072 — comprobante de ingreso: soporte para el cliente de que su vehículo ingresó. Disponible
 * para cualquier estado de la sesión (reimpresión desde Historial).
 */
@Service
public class EntryReceiptService {

    public record EntryReceiptData(
            String parkingName,
            String nit,
            String address,
            String phone,
            String entryNumber,
            String plate,
            String vehicleTypeName,
            Instant entryDateTime,
            String employeeName,
            String notes,
            ParkingStatus status
    ) {
    }

    private final ParkingSessionRepository parkingSessionRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final UserRepository userRepository;
    private final ConfigurationRepository configurationRepository;

    public EntryReceiptService(ParkingSessionRepository parkingSessionRepository,
                               VehicleTypeRepository vehicleTypeRepository, UserRepository userRepository,
                               ConfigurationRepository configurationRepository) {
        this.parkingSessionRepository = parkingSessionRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.userRepository = userRepository;
        this.configurationRepository = configurationRepository;
    }

    public EntryReceiptData getBySessionId(String sessionId) {
        ParkingSession session = parkingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Sesión de parqueo no encontrada"));
        String vehicleTypeName = vehicleTypeRepository.findById(session.vehicleTypeId())
                .map(VehicleType::name).orElse(null);
        String employeeName = userRepository.findById(session.entryEmployeeId())
                .map(User::fullName).orElse(null);
        Configuration configuration = configurationRepository.find().orElse(null);

        return new EntryReceiptData(
                configuration != null ? configuration.parkingName() : null,
                configuration != null ? configuration.nit() : null,
                configuration != null ? configuration.address() : null,
                configuration != null ? configuration.phone() : null,
                session.entryNumber(),
                session.plate(),
                vehicleTypeName,
                session.entryDateTime(),
                employeeName,
                session.notes(),
                session.status());
    }
}
