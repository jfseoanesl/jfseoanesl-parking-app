package com.parking.application.parking;

import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.ParkingStatus;
import com.parking.domain.model.Payment;
import com.parking.domain.model.Tariff;
import com.parking.domain.model.User;
import com.parking.domain.model.VehicleType;
import com.parking.domain.repository.ParkingSessionRepository;
import com.parking.domain.repository.PaymentRepository;
import com.parking.domain.repository.TariffRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.domain.repository.VehicleTypeRepository;
import com.parking.domain.tariffengine.StayDurationCalculator;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * RF-058: historial de sesiones de parqueo (activas, finalizadas, anuladas) con filtros por fecha,
 * placa, estado y empleado. El rango se aplica en la base (fecha de ingreso, zona horaria del
 * parqueadero, BR-026); el resto de filtros en memoria, suficiente para el volumen de un parqueadero.
 */
@Service
public class ParkingHistoryService {

    /** Tope de filas por consulta: con mas, se pide acotar el rango o los filtros. */
    public static final int MAX_RESULTS = 500;

    public record HistoryItem(
            String id,
            String plate,
            String vehicleTypeCode,
            String vehicleTypeName,
            ParkingStatus status,
            Instant entryDateTime,
            Instant exitDateTime,
            long elapsedMinutes,
            String entryEmployeeName,
            String exitEmployeeName,
            String appliedTariffName,
            BigDecimal chargedAmount,
            String operationNumber,
            String paymentId,
            String notes,
            String cancelReason,
            String entryNumber,
            /** BR-029: cliente de la mensualidad que cubrió la sesión (null = cliente normal). */
            String subscriptionCustomerName
    ) {
    }

    public record HistoryPage(List<HistoryItem> items, boolean truncated) {
    }

    private final ParkingSessionRepository parkingSessionRepository;
    private final PaymentRepository paymentRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final UserRepository userRepository;
    private final TariffRepository tariffRepository;
    private final TimeProvider timeProvider;
    private final com.parking.domain.repository.SubscriptionRepository subscriptionRepository;

    public ParkingHistoryService(ParkingSessionRepository parkingSessionRepository, PaymentRepository paymentRepository,
                                VehicleTypeRepository vehicleTypeRepository, UserRepository userRepository,
                                TariffRepository tariffRepository, TimeProvider timeProvider,
                                com.parking.domain.repository.SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.paymentRepository = paymentRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.userRepository = userRepository;
        this.tariffRepository = tariffRepository;
        this.timeProvider = timeProvider;
    }

    /**
     * @param from/to     rango de fechas de INGRESO, inclusivo (zona horaria del parqueadero)
     * @param plate       coincidencia parcial, sin distinguir mayusculas (opcional)
     * @param status      estado exacto (opcional)
     * @param employeeId  empleado que registro el ingreso o la salida (opcional)
     */
    public HistoryPage search(LocalDate from, LocalDate to, String plate, ParkingStatus status, String employeeId) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        ZoneId zone = timeProvider.zone();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);
        String plateFilter = plate == null || plate.isBlank() ? null : plate.trim().toUpperCase(Locale.ROOT);

        List<ParkingSession> matches = parkingSessionRepository.findByEntryDateTimeBetween(start, end).stream()
                .filter(s -> plateFilter == null || s.plate().contains(plateFilter))
                .filter(s -> status == null || s.status() == status)
                .filter(s -> employeeId == null || employeeId.equals(s.entryEmployeeId())
                        || employeeId.equals(s.exitEmployeeId()))
                .sorted(Comparator.comparing(ParkingSession::entryDateTime).reversed())
                .toList();

        boolean truncated = matches.size() > MAX_RESULTS;
        Lookups lookups = new Lookups();
        List<HistoryItem> items = matches.stream().limit(MAX_RESULTS).map(s -> toItem(s, lookups)).toList();
        return new HistoryPage(items, truncated);
    }

    /** Cache por consulta: evita repetir la misma busqueda de usuario/tipo/tarifa en cada fila. */
    private final class Lookups {
        final Map<String, Optional<User>> users = new HashMap<>();
        final Map<String, Optional<VehicleType>> vehicleTypes = new HashMap<>();
        final Map<String, Optional<Tariff>> tariffs = new HashMap<>();
        final Map<String, Optional<com.parking.domain.model.Subscription>> subscriptions = new HashMap<>();

        String subscriptionName(String id) {
            return id == null ? null : cached(subscriptions, id, subscriptionRepository::findById)
                    .map(com.parking.domain.model.Subscription::customerName).orElse(null);
        }

        String userName(String id) {
            return id == null ? null : cached(users, id, userRepository::findById).map(User::fullName).orElse(null);
        }

        Optional<VehicleType> vehicleType(String id) {
            return cached(vehicleTypes, id, vehicleTypeRepository::findById);
        }

        String tariffName(String id) {
            return id == null ? null : cached(tariffs, id, tariffRepository::findById).map(Tariff::name).orElse(null);
        }

        private <T> Optional<T> cached(Map<String, Optional<T>> cache, String id, Function<String, Optional<T>> loader) {
            return cache.computeIfAbsent(id, loader);
        }
    }

    private HistoryItem toItem(ParkingSession s, Lookups lookups) {
        Optional<VehicleType> vehicleType = lookups.vehicleType(s.vehicleTypeId());
        Optional<Payment> payment = s.status() == ParkingStatus.FINALIZADO
                ? paymentRepository.findByParkingSessionId(s.id()) : Optional.empty();
        Instant referenceEnd = s.exitDateTime() != null ? s.exitDateTime()
                : s.status() == ParkingStatus.EN_PARQUEADERO ? timeProvider.now() : s.entryDateTime();
        return new HistoryItem(
                s.id(), s.plate(),
                vehicleType.map(VehicleType::code).orElse(null),
                vehicleType.map(VehicleType::name).orElse(null),
                s.status(), s.entryDateTime(), s.exitDateTime(),
                StayDurationCalculator.elapsedMinutes(s.entryDateTime(), referenceEnd),
                lookups.userName(s.entryEmployeeId()),
                lookups.userName(s.exitEmployeeId()),
                lookups.tariffName(s.appliedTariffId()),
                payment.map(Payment::chargedAmount).orElse(null),
                payment.map(Payment::operationNumber).orElse(null),
                payment.map(Payment::id).orElse(null),
                s.notes(), s.cancelReason(), s.entryNumber(), lookups.subscriptionName(s.subscriptionId()));
    }
}
