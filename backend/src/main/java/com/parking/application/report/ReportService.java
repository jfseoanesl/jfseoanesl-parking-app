package com.parking.application.report;

import com.parking.domain.model.CashSession;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.Payment;
import com.parking.domain.model.Tariff;
import com.parking.domain.model.User;
import com.parking.domain.model.VehicleType;
import com.parking.domain.repository.CashSessionRepository;
import com.parking.domain.repository.ParkingSessionRepository;
import com.parking.domain.repository.PaymentMethodRepository;
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
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UC-010 (RF-060 a RF-063). El "reporte diario/semanal/mensual/personalizado" del prompt es en
 * realidad un unico calculo parametrizado por rango de fechas (prompt.md §13) — no cuatro
 * implementaciones distintas.
 */
@Service
public class ReportService {

    private final ParkingSessionRepository parkingSessionRepository;
    private final PaymentRepository paymentRepository;
    private final CashSessionRepository cashSessionRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final TariffRepository tariffRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;
    private final TimeProvider timeProvider;
    private final com.parking.domain.repository.SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final com.parking.domain.repository.CashTransactionRepository cashTransactionRepository;

    public ReportService(ParkingSessionRepository parkingSessionRepository, PaymentRepository paymentRepository,
                          CashSessionRepository cashSessionRepository, VehicleTypeRepository vehicleTypeRepository,
                          TariffRepository tariffRepository, PaymentMethodRepository paymentMethodRepository,
                          UserRepository userRepository, TimeProvider timeProvider,
                          com.parking.domain.repository.SubscriptionPaymentRepository subscriptionPaymentRepository,
                          com.parking.domain.repository.CashTransactionRepository cashTransactionRepository) {
        this.cashTransactionRepository = cashTransactionRepository;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.paymentRepository = paymentRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.tariffRepository = tariffRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.userRepository = userRepository;
        this.timeProvider = timeProvider;
    }

    public ReportData generate(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }
        ZoneId zone = timeProvider.zone();
        Instant fromInstant = from.atStartOfDay(zone).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(zone).toInstant(); // limite superior exclusivo

        List<ParkingSession> entered = parkingSessionRepository.findByEntryDateTimeBetween(fromInstant, toInstant);
        List<ParkingSession> finalized =
                parkingSessionRepository.findFinalizedByExitDateTimeBetween(fromInstant, toInstant);
        List<ParkingSession> cancelled =
                parkingSessionRepository.findCancelledByCancelledAtBetween(fromInstant, toInstant);
        List<Payment> payments = paymentRepository.findByPaidAtBetween(fromInstant, toInstant);
        var subscriptionPayments = subscriptionPaymentRepository.findByPaidAtBetween(fromInstant, toInstant);
        List<CashSession> closedCashSessions =
                cashSessionRepository.findClosedByClosedAtBetween(fromInstant, toInstant);

        Map<String, String> vehicleTypeNames = new HashMap<>();
        Map<String, String> tariffNames = new HashMap<>();
        Map<String, String> paymentMethodNames = new HashMap<>();
        Map<String, String> employeeNames = new HashMap<>();

        Map<String, Long> enteredByVehicleType = entered.stream()
                .collect(Collectors.groupingBy(
                        s -> nameOf(vehicleTypeNames, s.vehicleTypeId(),
                                id -> vehicleTypeRepository.findById(id).map(VehicleType::name).orElse("—")),
                        Collectors.counting()));

        BigDecimal subscriptionCollected = subscriptionPayments.stream()
                .map(com.parking.domain.model.SubscriptionPayment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollected = payments.stream()
                .map(Payment::chargedAmount).reduce(BigDecimal.ZERO, BigDecimal::add).add(subscriptionCollected);

        Map<String, BigDecimal> collectedByPaymentMethod = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> nameOf(paymentMethodNames, p.paymentMethodId(),
                                id -> paymentMethodRepository.findById(id).map(pm -> pm.name()).orElse("—")),
                        Collectors.reducing(BigDecimal.ZERO, Payment::chargedAmount, BigDecimal::add)));

        Map<String, BigDecimal> collectedByEmployee = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> nameOf(employeeNames, p.userId(),
                                id -> userRepository.findById(id).map(User::fullName).orElse("—")),
                        Collectors.reducing(BigDecimal.ZERO, Payment::chargedAmount, BigDecimal::add)));

        // BR-031: las mensualidades cuentan en el desglose por medio de pago y por empleado.
        Map<String, BigDecimal> byMethod = new HashMap<>(collectedByPaymentMethod);
        Map<String, BigDecimal> byEmployee = new HashMap<>(collectedByEmployee);
        for (var p : subscriptionPayments) {
            byMethod.merge(nameOf(paymentMethodNames, p.paymentMethodId(),
                    id -> paymentMethodRepository.findById(id).map(pm -> pm.name()).orElse("—")), p.amount(), BigDecimal::add);
            byEmployee.merge(nameOf(employeeNames, p.userId(),
                    id -> userRepository.findById(id).map(User::fullName).orElse("—")), p.amount(), BigDecimal::add);
        }
        // RF-083 / BR-035: los otros ingresos activos tambien son recaudo; los gastos se restan en el neto.
        var transactions = cashTransactionRepository.findByCreatedAtBetween(fromInstant, toInstant).stream()
                .filter(com.parking.domain.model.CashTransaction::isActive).toList();
        BigDecimal otherIncomeCollected = BigDecimal.ZERO;
        BigDecimal expensesTotal = BigDecimal.ZERO;
        for (var t : transactions) {
            if (t.type() == com.parking.domain.model.CashTransactionType.EXPENSE) {
                expensesTotal = expensesTotal.add(t.amount());
                continue;
            }
            otherIncomeCollected = otherIncomeCollected.add(t.amount());
            byMethod.merge(nameOf(paymentMethodNames, t.paymentMethodId(),
                    id -> paymentMethodRepository.findById(id).map(pm -> pm.name()).orElse("—")), t.amount(), BigDecimal::add);
            byEmployee.merge(nameOf(employeeNames, t.userId(),
                    id -> userRepository.findById(id).map(User::fullName).orElse("—")), t.amount(), BigDecimal::add);
        }
        totalCollected = totalCollected.add(otherIncomeCollected);
        collectedByPaymentMethod = byMethod;
        collectedByEmployee = byEmployee;
        long subscriberExits = finalized.stream().filter(s -> s.subscriptionId() != null && s.appliedTariffId() == null).count();

        Map<String, Payment> paymentBySession = payments.stream()
                .collect(Collectors.toMap(Payment::parkingSessionId, p -> p, (a, b) -> a));
        Map<String, BigDecimal> collectedByTariff = finalized.stream()
                .filter(s -> s.appliedTariffId() != null && paymentBySession.containsKey(s.id()))
                .collect(Collectors.groupingBy(
                        s -> nameOf(tariffNames, s.appliedTariffId(),
                                id -> tariffRepository.findById(id).map(Tariff::name).orElse("—")),
                        Collectors.reducing(BigDecimal.ZERO,
                                s -> paymentBySession.get(s.id()).chargedAmount(), BigDecimal::add)));

        Double averageDurationMinutes = finalized.isEmpty() ? null : finalized.stream()
                .mapToLong(s -> StayDurationCalculator.elapsedMinutes(s.entryDateTime(), s.exitDateTime()))
                .average().orElse(0);

        Integer peakHour = entered.isEmpty() ? null : entered.stream()
                .collect(Collectors.groupingBy(s -> ZonedDateTime.ofInstant(s.entryDateTime(), zone).getHour(),
                        Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        BigDecimal cashDifferencesSum = closedCashSessions.stream()
                .map(CashSession::difference)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportData(from, to, entered.size(), finalized.size(), enteredByVehicleType, totalCollected,
                collectedByPaymentMethod, collectedByTariff, averageDurationMinutes, peakHour, collectedByEmployee,
                cancelled.size(), cashDifferencesSum, subscriptionCollected, subscriptionPayments.size(), subscriberExits,
                otherIncomeCollected, expensesTotal, totalCollected.subtract(expensesTotal));
    }

    private static String nameOf(Map<String, String> cache, String id, java.util.function.Function<String, String> resolver) {
        return cache.computeIfAbsent(id, resolver);
    }
}
