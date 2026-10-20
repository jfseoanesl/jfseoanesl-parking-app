package com.parking.application.subscription;

import com.parking.application.audit.AuditService;
import com.parking.domain.exception.BusinessRuleViolationException;
import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.CashMovement;
import com.parking.domain.model.CashMovementType;
import com.parking.domain.model.CashSession;
import com.parking.domain.model.Configuration;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.PaymentMethod;
import com.parking.domain.model.PlateNormalizer;
import com.parking.domain.model.Subscription;
import com.parking.domain.model.SubscriptionPayment;
import com.parking.domain.model.SubscriptionPeriods;
import com.parking.domain.model.SubscriptionStatus;
import com.parking.domain.model.SubscriptionVehicle;
import com.parking.domain.model.User;
import com.parking.domain.model.VehicleType;
import com.parking.domain.repository.CashMovementRepository;
import com.parking.domain.repository.CashSessionRepository;
import com.parking.domain.repository.ConfigurationRepository;
import com.parking.domain.repository.ParkingSessionRepository;
import com.parking.domain.repository.PaymentMethodRepository;
import com.parking.domain.repository.PaymentRepository;
import com.parking.domain.repository.SubscriptionPaymentRepository;
import com.parking.domain.repository.SubscriptionRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.domain.repository.VehicleTypeRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.parking.application.audit.AuditJson.num;
import static com.parking.application.audit.AuditJson.str;

/**
 * RF-074 a RF-077 / BR-028 a BR-031: mensualidades. Gestión (solo administrador), estado de pago,
 * reconocimiento de placas al ingreso, cobertura de la salida y cobro mensual en caja.
 */
@Service
public class SubscriptionService {

    public record VehicleInput(String plate, String vehicleTypeId) {
    }

    public record SubscriptionInput(String customerName, String documentId, String phone, BigDecimal monthlyFee,
                                    Integer cutOffDay, String notes, List<VehicleInput> vehicles) {
    }

    /** Mensualidad con su estado (BR-028) y el vehículo que tiene dentro, si hay uno. */
    public record SubscriptionView(Subscription subscription, SubscriptionStatus status, String vehicleInsidePlate) {
    }

    /** BR-030: lo que la pantalla de salida necesita saber de la mensualidad de una sesión. */
    public record Coverage(String subscriptionId, String customerName, SubscriptionStatus status,
                           LocalDate paidUntil, boolean covered) {
    }

    public record PaymentReceipt(String parkingName, String nit, String address, String phone,
                                 String operationNumber, String customerName, String documentId,
                                 List<String> plates, LocalDate periodFrom, LocalDate periodTo, BigDecimal amount,
                                 String paymentMethodName, String employeeName, Instant paidAt) {
    }

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentRepository paymentRepository;
    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final ConfigurationRepository configurationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionPaymentRepository subscriptionPaymentRepository,
                               VehicleTypeRepository vehicleTypeRepository,
                               PaymentMethodRepository paymentMethodRepository, PaymentRepository paymentRepository,
                               CashSessionRepository cashSessionRepository,
                               CashMovementRepository cashMovementRepository,
                               ParkingSessionRepository parkingSessionRepository,
                               ConfigurationRepository configurationRepository, UserRepository userRepository,
                               AuditService auditService, TimeProvider timeProvider) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentRepository = paymentRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.configurationRepository = configurationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    // --- Consulta -----------------------------------------------------------------------------

    public List<SubscriptionView> list() {
        int graceDays = graceDays();
        LocalDate today = today();
        return subscriptionRepository.findAll().stream().map(s -> view(s, graceDays, today)).toList();
    }

    public SubscriptionView get(String id) {
        return view(find(id), graceDays(), today());
    }

    public List<SubscriptionPayment> payments(String subscriptionId) {
        find(subscriptionId);
        return subscriptionPaymentRepository.findBySubscriptionId(subscriptionId);
    }

    public SubscriptionStatus statusOf(Subscription s) {
        return SubscriptionPeriods.statusOf(s.active(), s.paidUntil(), graceDays(), today());
    }

    /** BR-029: mensualidad activa a la que pertenece la placa (ya normalizada), si hay alguna. */
    public Optional<Subscription> findActiveByPlate(String normalizedPlate) {
        return subscriptionRepository.findVehicleByPlate(normalizedPlate)
                .flatMap(v -> subscriptionRepository.findById(v.subscriptionId()))
                .filter(Subscription::active);
    }

    /** BR-030: cobertura de la salida de una sesión; vacío si la sesión no es de un cliente mensual. */
    public Optional<Coverage> coverageOf(ParkingSession session) {
        if (session.subscriptionId() == null) return Optional.empty();
        return subscriptionRepository.findById(session.subscriptionId()).map(s -> {
            SubscriptionStatus status = statusOf(s);
            return new Coverage(s.id(), s.customerName(), status, s.paidUntil(), status.coversExit());
        });
    }

    // --- Gestión (administrador) -------------------------------------------------------------

    @Transactional
    public Subscription create(SubscriptionInput input, String actorId) {
        Instant now = timeProvider.now();
        String id = UUID.randomUUID().toString();
        Subscription subscription = new Subscription(id, input.customerName().trim(), blankToNull(input.documentId()),
                blankToNull(input.phone()), input.monthlyFee(), input.cutOffDay(), null, true,
                blankToNull(input.notes()), now, now, actorId, vehicles(id, input));
        validate(subscription);
        Subscription saved = subscriptionRepository.save(subscription);
        auditService.record(actorId, "SUBSCRIPTION_CREATED", "Subscription", id, null, describe(saved));
        return saved;
    }

    @Transactional
    public Subscription update(String id, SubscriptionInput input, String actorId) {
        Subscription existing = find(id);
        Subscription updated = new Subscription(id, input.customerName().trim(), blankToNull(input.documentId()),
                blankToNull(input.phone()), input.monthlyFee(), input.cutOffDay(), existing.paidUntil(),
                existing.active(), blankToNull(input.notes()), existing.createdAt(), timeProvider.now(),
                existing.createdBy(), vehicles(id, input));
        validate(updated);
        Subscription saved = subscriptionRepository.save(updated);
        auditService.record(actorId, "SUBSCRIPTION_UPDATED", "Subscription", id, describe(existing), describe(saved));
        return saved;
    }

    @Transactional
    public Subscription setActive(String id, boolean active, String actorId) {
        Subscription existing = find(id);
        Subscription updated = new Subscription(id, existing.customerName(), existing.documentId(), existing.phone(),
                existing.monthlyFee(), existing.cutOffDay(), existing.paidUntil(), active, existing.notes(),
                existing.createdAt(), timeProvider.now(), existing.createdBy(), null);
        Subscription saved = subscriptionRepository.save(updated);
        auditService.record(actorId, active ? "SUBSCRIPTION_ACTIVATED" : "SUBSCRIPTION_DEACTIVATED", "Subscription", id,
                describe(existing), describe(saved));
        return saved;
    }

    // --- Cobro mensual en caja (BR-031) ------------------------------------------------------

    @Transactional
    public SubscriptionPayment registerPayment(String subscriptionId, String paymentMethodId, BigDecimal amount,
                                               String employeeId) {
        Subscription subscription = find(subscriptionId);
        if (!subscription.active()) {
            throw new BusinessRuleViolationException("SUBSCRIPTION_INACTIVE", "La mensualidad está inactiva");
        }
        paymentMethodRepository.findById(paymentMethodId).filter(PaymentMethod::active)
                .orElseThrow(() -> new NotFoundException("Medio de pago no encontrado o inactivo"));
        CashSession cashSession = cashSessionRepository.findOpenByEmployeeId(employeeId)
                .orElseThrow(() -> new BusinessRuleViolationException("NO_OPEN_CASH_SESSION",
                        "Debe abrir caja antes de registrar cobros"));
        if (amount == null || amount.compareTo(subscription.monthlyFee()) != 0) {
            throw new BusinessRuleViolationException("CHARGED_AMOUNT_MISMATCH",
                    "El valor de la mensualidad es $" + subscription.monthlyFee().toPlainString()
                            + ". Revisa el valor y confirma de nuevo.");
        }

        LocalDate from = SubscriptionPeriods.nextPeriodStart(subscription.paidUntil(), today());
        LocalDate to = SubscriptionPeriods.periodEnd(from, subscription.cutOffDay());
        Instant now = timeProvider.now();
        // Mismo consecutivo OP- que los cobros de salida: un número nunca se repite entre ambos.
        String operationNumber = "OP-" + String.format("%06d", paymentRepository.nextOperationSequence());
        SubscriptionPayment payment = subscriptionPaymentRepository.save(new SubscriptionPayment(
                UUID.randomUUID().toString(), subscriptionId, cashSession.id(), paymentMethodId, employeeId,
                amount, from, to, operationNumber, now));

        subscriptionRepository.save(new Subscription(subscription.id(), subscription.customerName(),
                subscription.documentId(), subscription.phone(), subscription.monthlyFee(), subscription.cutOffDay(),
                to, true, subscription.notes(), subscription.createdAt(), now, subscription.createdBy(), null));

        cashMovementRepository.save(new CashMovement(UUID.randomUUID().toString(), cashSession.id(), null,
                CashMovementType.PAYMENT, amount, "Mensualidad " + operationNumber + " - " + subscription.customerName(),
                now));
        return payment;
    }

    public PaymentReceipt receipt(String paymentId) {
        SubscriptionPayment payment = subscriptionPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Pago de mensualidad no encontrado"));
        Subscription subscription = find(payment.subscriptionId());
        Configuration configuration = configurationRepository.find().orElse(null);
        String methodName = paymentMethodRepository.findById(payment.paymentMethodId()).map(PaymentMethod::name).orElse(null);
        String employeeName = userRepository.findById(payment.userId()).map(User::fullName).orElse(null);
        return new PaymentReceipt(
                configuration != null ? configuration.parkingName() : null,
                configuration != null ? configuration.nit() : null,
                configuration != null ? configuration.address() : null,
                configuration != null ? configuration.phone() : null,
                payment.operationNumber(), subscription.customerName(), subscription.documentId(),
                subscription.vehicles().stream().map(SubscriptionVehicle::plate).toList(),
                payment.periodFrom(), payment.periodTo(), payment.amount(), methodName, employeeName, payment.paidAt());
    }

    // --- Apoyo -------------------------------------------------------------------------------

    private SubscriptionView view(Subscription s, int graceDays, LocalDate today) {
        String inside = parkingSessionRepository.findActiveBySubscriptionId(s.id()).map(ParkingSession::plate).orElse(null);
        return new SubscriptionView(s, SubscriptionPeriods.statusOf(s.active(), s.paidUntil(), graceDays, today), inside);
    }

    private Subscription find(String id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mensualidad no encontrada"));
    }

    private List<SubscriptionVehicle> vehicles(String subscriptionId, SubscriptionInput input) {
        List<SubscriptionVehicle> result = new ArrayList<>();
        if (input.vehicles() == null) return result;
        for (VehicleInput v : input.vehicles()) {
            if (v == null || v.plate() == null || v.plate().isBlank()) continue;
            result.add(new SubscriptionVehicle(UUID.randomUUID().toString(), subscriptionId,
                    PlateNormalizer.normalize(v.plate()), v.vehicleTypeId()));
        }
        return result;
    }

    private void validate(Subscription s) {
        if (s.customerName().isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        if (s.monthlyFee() == null || s.monthlyFee().signum() <= 0) {
            throw new IllegalArgumentException("El valor mensual debe ser mayor que cero");
        }
        if (s.cutOffDay() < 1 || s.cutOffDay() > 28) {
            throw new IllegalArgumentException("El día de corte debe estar entre 1 y 28");
        }
        if (s.vehicles().isEmpty()) {
            throw new IllegalArgumentException("Registre al menos una placa autorizada");
        }
        Set<String> seen = new HashSet<>();
        for (SubscriptionVehicle v : s.vehicles()) {
            if (!seen.add(v.plate())) {
                throw new IllegalArgumentException("La placa " + v.plate() + " está repetida");
            }
            vehicleTypeRepository.findById(v.vehicleTypeId()).filter(VehicleType::active)
                    .orElseThrow(() -> new NotFoundException("Tipo de vehículo no encontrado o inactivo"));
            subscriptionRepository.findVehicleByPlate(v.plate())
                    .filter(existing -> !existing.subscriptionId().equals(s.id()))
                    .ifPresent(existing -> {
                        throw new BusinessRuleViolationException("PLATE_IN_OTHER_SUBSCRIPTION",
                                "La placa " + v.plate() + " ya pertenece a otra mensualidad");
                    });
        }
    }

    private int graceDays() {
        return configurationRepository.find().map(Configuration::subscriptionGraceDays).orElse(3);
    }

    private LocalDate today() {
        return timeProvider.now().atZone(timeProvider.zone()).toLocalDate();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String describe(Subscription s) {
        String plates = s.vehicles() == null ? "[]" : s.vehicles().stream().map(v -> str(v.plate()))
                .collect(Collectors.joining(",", "[", "]"));
        return "{\"customerName\":" + str(s.customerName())
                + ",\"documentId\":" + str(s.documentId())
                + ",\"phone\":" + str(s.phone())
                + ",\"monthlyFee\":" + num(s.monthlyFee())
                + ",\"cutOffDay\":" + s.cutOffDay()
                + ",\"paidUntil\":" + str(s.paidUntil() != null ? s.paidUntil().toString() : null)
                + ",\"active\":" + s.active()
                + ",\"plates\":" + plates + "}";
    }
}
