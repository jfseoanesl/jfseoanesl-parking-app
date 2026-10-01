package com.parking.application.parking;

import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.Tariff;
import com.parking.domain.model.User;
import com.parking.domain.model.VehicleType;
import com.parking.domain.repository.ParkingSessionRepository;
import com.parking.domain.repository.TariffRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.domain.repository.VehicleTypeRepository;
import com.parking.domain.tariffengine.StayDurationCalculator;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** RF-030 a RF-033 (vehículos activos) y detalle de sesión. */
@Service
public class ParkingSessionQueryService {

    private final ParkingSessionRepository parkingSessionRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final UserRepository userRepository;
    private final TariffRepository tariffRepository;
    private final TimeProvider timeProvider;

    public ParkingSessionQueryService(ParkingSessionRepository parkingSessionRepository,
                                       VehicleTypeRepository vehicleTypeRepository, UserRepository userRepository,
                                       TariffRepository tariffRepository, TimeProvider timeProvider) {
        this.parkingSessionRepository = parkingSessionRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.userRepository = userRepository;
        this.tariffRepository = tariffRepository;
        this.timeProvider = timeProvider;
    }

    public List<ParkingSessionView> listActive(String plateFilter) {
        return parkingSessionRepository.findAllActive(plateFilter).stream().map(this::toView).toList();
    }

    public ParkingSessionView getById(String id) {
        ParkingSession session = parkingSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sesión de parqueo no encontrada"));
        return toView(session);
    }

    private ParkingSessionView toView(ParkingSession session) {
        VehicleType vehicleType = vehicleTypeRepository.findById(session.vehicleTypeId()).orElse(null);
        User entryEmployee = userRepository.findById(session.entryEmployeeId()).orElse(null);
        String tariffName = session.appliedTariffId() != null
                ? tariffRepository.findById(session.appliedTariffId()).map(Tariff::name).orElse(null)
                : null;

        Instant referenceExit = session.exitDateTime() != null ? session.exitDateTime() : timeProvider.now();
        long elapsedMinutes = StayDurationCalculator.elapsedMinutes(session.entryDateTime(), referenceExit);

        return new ParkingSessionView(
                session.id(),
                session.plate(),
                vehicleType != null ? vehicleType.code() : null,
                vehicleType != null ? vehicleType.name() : null,
                session.entryDateTime(),
                session.exitDateTime(),
                elapsedMinutes,
                entryEmployee != null ? entryEmployee.fullName() : null,
                session.status(),
                tariffName,
                session.calculatedAmount(),
                session.notes(),
                session.cancelReason()
        );
    }
}
