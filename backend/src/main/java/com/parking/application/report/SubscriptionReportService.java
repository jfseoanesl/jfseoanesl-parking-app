package com.parking.application.report;

import com.parking.application.subscription.SubscriptionService;
import com.parking.application.subscription.SubscriptionService.SubscriptionView;
import com.parking.domain.model.PaymentMethod;
import com.parking.domain.model.Subscription;
import com.parking.domain.model.SubscriptionPayment;
import com.parking.domain.model.SubscriptionStatus;
import com.parking.domain.model.SubscriptionVehicle;
import com.parking.domain.model.User;
import com.parking.domain.repository.PaymentMethodRepository;
import com.parking.domain.repository.SubscriptionPaymentRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * RF-078: reporte de mensualidades (solo administrador). Estado de cartera (estado BR-028 a hoy),
 * pagos cobrados en el período e indicadores, con filtros por estado y por cliente/documento/placa.
 */
@Service
public class SubscriptionReportService {

    public record Filter(LocalDate from, LocalDate to, Set<SubscriptionStatus> statuses, String query) {
    }

    public record PortfolioRow(String subscriptionId, String customerName, String documentId, String phone,
                               List<String> plates, BigDecimal monthlyFee, int cutOffDay, LocalDate paidUntil,
                               SubscriptionStatus status, Long daysOverdue, BigDecimal paidInPeriod,
                               long paymentsInPeriod, String vehicleInsidePlate) {
    }

    public record PaymentRow(String paymentId, Instant paidAt, String operationNumber, String customerName,
                             String documentId, LocalDate periodFrom, LocalDate periodTo, BigDecimal amount,
                             String paymentMethodName, String employeeName) {
    }

    public record Report(LocalDate from, LocalDate to, LocalDate today, BigDecimal collected, long paymentsCount,
                         long activeCount, Map<SubscriptionStatus, Long> countByStatus, BigDecimal overdueAmount,
                         List<PortfolioRow> portfolio, List<PaymentRow> payments) {
    }

    private final SubscriptionService subscriptionService;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;
    private final TimeProvider timeProvider;

    public SubscriptionReportService(SubscriptionService subscriptionService,
                                     SubscriptionPaymentRepository subscriptionPaymentRepository,
                                     PaymentMethodRepository paymentMethodRepository, UserRepository userRepository,
                                     TimeProvider timeProvider) {
        this.subscriptionService = subscriptionService;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.userRepository = userRepository;
        this.timeProvider = timeProvider;
    }

    public Report generate(Filter filter) {
        if (filter.to().isBefore(filter.from())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }
        ZoneId zone = timeProvider.zone();
        LocalDate today = timeProvider.now().atZone(zone).toLocalDate();
        Instant fromInstant = filter.from().atStartOfDay(zone).toInstant();
        Instant toInstant = filter.to().plusDays(1).atStartOfDay(zone).toInstant();

        List<SubscriptionView> selected = subscriptionService.list().stream()
                .filter(v -> filter.statuses() == null || filter.statuses().isEmpty() || filter.statuses().contains(v.status()))
                .filter(v -> matches(v.subscription(), filter.query()))
                .toList();
        Map<String, Subscription> byId = new HashMap<>();
        selected.forEach(v -> byId.put(v.subscription().id(), v.subscription()));

        List<SubscriptionPayment> payments = subscriptionPaymentRepository.findByPaidAtBetween(fromInstant, toInstant)
                .stream().filter(p -> byId.containsKey(p.subscriptionId())).toList();
        Map<String, List<SubscriptionPayment>> paymentsBySubscription = new HashMap<>();
        payments.forEach(p -> paymentsBySubscription.computeIfAbsent(p.subscriptionId(), k -> new java.util.ArrayList<>()).add(p));

        List<PortfolioRow> portfolio = selected.stream().map(v -> {
            Subscription s = v.subscription();
            List<SubscriptionPayment> own = paymentsBySubscription.getOrDefault(s.id(), List.of());
            Long daysOverdue = s.paidUntil() != null && today.isAfter(s.paidUntil())
                    && (v.status() == SubscriptionStatus.EN_GRACIA || v.status() == SubscriptionStatus.VENCIDA)
                    ? ChronoUnit.DAYS.between(s.paidUntil(), today) : null;
            return new PortfolioRow(s.id(), s.customerName(), s.documentId(), s.phone(),
                    s.vehicles().stream().map(SubscriptionVehicle::plate).toList(), s.monthlyFee(), s.cutOffDay(),
                    s.paidUntil(), v.status(), daysOverdue,
                    own.stream().map(SubscriptionPayment::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                    own.size(), v.vehicleInsidePlate());
        }).sorted(Comparator
                .comparingInt((PortfolioRow r) -> severity(r.status()))
                .thenComparing((PortfolioRow r) -> r.daysOverdue() == null ? 0L : -r.daysOverdue())
                .thenComparing(PortfolioRow::customerName, String.CASE_INSENSITIVE_ORDER)).toList();

        Map<String, String> methodNames = new HashMap<>();
        Map<String, String> userNames = new HashMap<>();
        List<PaymentRow> paymentRows = payments.stream()
                .sorted(Comparator.comparing(SubscriptionPayment::paidAt).reversed())
                .map(p -> {
                    Subscription s = byId.get(p.subscriptionId());
                    return new PaymentRow(p.id(), p.paidAt(), p.operationNumber(), s.customerName(), s.documentId(),
                            p.periodFrom(), p.periodTo(), p.amount(),
                            cached(methodNames, p.paymentMethodId(),
                                    id -> paymentMethodRepository.findById(id).map(PaymentMethod::name).orElse("—")),
                            cached(userNames, p.userId(),
                                    id -> userRepository.findById(id).map(User::fullName).orElse("—")));
                }).toList();

        Map<SubscriptionStatus, Long> countByStatus = new EnumMap<>(SubscriptionStatus.class);
        for (SubscriptionStatus status : SubscriptionStatus.values()) countByStatus.put(status, 0L);
        selected.forEach(v -> countByStatus.merge(v.status(), 1L, Long::sum));

        BigDecimal overdueAmount = selected.stream()
                .filter(v -> v.status() == SubscriptionStatus.VENCIDA || v.status() == SubscriptionStatus.SIN_PAGO)
                .map(v -> v.subscription().monthlyFee()).reduce(BigDecimal.ZERO, BigDecimal::add);
        long active = selected.stream().filter(v -> v.subscription().active()).count();

        return new Report(filter.from(), filter.to(), today,
                payments.stream().map(SubscriptionPayment::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                payments.size(), active, new LinkedHashMap<>(countByStatus), overdueAmount, portfolio, paymentRows);
    }

    /** Cliente, documento o placa (normalizada, BR-002), sin distinguir mayúsculas. */
    static boolean matches(Subscription s, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.trim().toLowerCase();
        // Misma normalizacion que BR-002, sin validar formato: la busqueda es texto libre.
        String plate = query.trim().toUpperCase().replaceAll("\\s+", "");
        return s.customerName().toLowerCase().contains(q)
                || (s.documentId() != null && s.documentId().toLowerCase().contains(q))
                || s.vehicles().stream().anyMatch(v -> v.plate().contains(plate));
    }

    /** Orden de la cartera: primero lo que requiere gestión. */
    private static int severity(SubscriptionStatus status) {
        return switch (status) {
            case VENCIDA -> 0;
            case SIN_PAGO -> 1;
            case EN_GRACIA -> 2;
            case AL_DIA -> 3;
            case INACTIVA -> 4;
        };
    }

    private static String cached(Map<String, String> cache, String id, Function<String, String> resolver) {
        return cache.computeIfAbsent(id, resolver);
    }
}
