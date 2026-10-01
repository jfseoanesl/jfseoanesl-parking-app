package com.parking.application.payment;

import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.Configuration;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.Payment;
import com.parking.domain.model.PaymentMethod;
import com.parking.domain.model.Tariff;
import com.parking.domain.model.User;
import com.parking.domain.model.VehicleType;
import com.parking.domain.repository.ConfigurationRepository;
import com.parking.domain.repository.ParkingSessionRepository;
import com.parking.domain.repository.PaymentMethodRepository;
import com.parking.domain.repository.PaymentRepository;
import com.parking.domain.repository.TariffRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.domain.repository.VehicleTypeRepository;
import com.parking.domain.tariffengine.StayDurationCalculator;
import org.springframework.stereotype.Service;

/** RF-050 — comprobante de pago, para impresión y visualización digital (prompt.md §11). */
@Service
public class ReceiptService {

    private final PaymentRepository paymentRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final TariffRepository tariffRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;
    private final ConfigurationRepository configurationRepository;

    public ReceiptService(PaymentRepository paymentRepository, ParkingSessionRepository parkingSessionRepository,
                           VehicleTypeRepository vehicleTypeRepository, TariffRepository tariffRepository,
                           PaymentMethodRepository paymentMethodRepository, UserRepository userRepository,
                           ConfigurationRepository configurationRepository) {
        this.paymentRepository = paymentRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.tariffRepository = tariffRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.userRepository = userRepository;
        this.configurationRepository = configurationRepository;
    }

    public ReceiptData getByPaymentId(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));
        ParkingSession session = parkingSessionRepository.findById(payment.parkingSessionId())
                .orElseThrow(() -> new NotFoundException("Sesión de parqueo no encontrada"));
        VehicleType vehicleType = vehicleTypeRepository.findById(session.vehicleTypeId())
                .orElseThrow(() -> new NotFoundException("Tipo de vehículo no encontrado"));
        PaymentMethod paymentMethod = paymentMethodRepository.findById(payment.paymentMethodId())
                .orElseThrow(() -> new NotFoundException("Medio de pago no encontrado"));
        User employee = userRepository.findById(payment.userId())
                .orElseThrow(() -> new NotFoundException("Empleado no encontrado"));
        String tariffName = session.appliedTariffId() != null
                ? tariffRepository.findById(session.appliedTariffId()).map(Tariff::name).orElse("—")
                : "—";
        Configuration configuration = configurationRepository.find().orElse(null);

        long elapsedMinutes = session.exitDateTime() != null
                ? StayDurationCalculator.elapsedMinutes(session.entryDateTime(), session.exitDateTime())
                : 0;

        return new ReceiptData(
                configuration != null ? configuration.parkingName() : null,
                configuration != null ? configuration.nit() : null,
                configuration != null ? configuration.address() : null,
                configuration != null ? configuration.phone() : null,
                session.plate(),
                vehicleType.name(),
                session.entryDateTime(),
                session.exitDateTime(),
                elapsedMinutes,
                tariffName,
                payment.chargedAmount(),
                paymentMethod.name(),
                payment.operationNumber(),
                employee.fullName()
        );
    }
}
