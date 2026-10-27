package com.parking.application.cash;

import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.CashSession;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.Payment;
import com.parking.domain.model.PaymentMethod;
import com.parking.domain.model.Tariff;
import com.parking.domain.model.User;
import com.parking.domain.repository.CashSessionRepository;
import com.parking.domain.repository.ParkingSessionRepository;
import com.parking.domain.repository.PaymentMethodRepository;
import com.parking.domain.repository.PaymentRepository;
import com.parking.domain.repository.TariffRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/** Consultas de caja: operaciones del turno (RF-059) y cierres para corregir (RF-057). */
@Service
public class CashOperationsService {

    /** Un cobro registrado en una caja. */
    public record CashOperation(
            String paymentId,
            String operationNumber,
            String plate,
            Instant paidAt,
            BigDecimal chargedAmount,
            String paymentMethodName,
            String tariffName,
            /**
             * PARKING = cobro de salida (comprobante /comprobantes/{paymentId}); SUBSCRIPTION = mensualidad;
             * INCOME / EXPENSE = otro ingreso o gasto de caja (UC-014, /comprobantes-movimiento/{paymentId}).
             */
            String kind,
            /** ACTIVE, o VOIDED para un ingreso/gasto anulado (BR-035). */
            String status,
            String voidReason
    ) {
    }

    /** Una caja cerrada, con el nombre del empleado (para la corrección administrativa, RF-057). */
    public record ClosedCashSession(
            String id,
            String employeeId,
            String employeeName,
            Instant openedAt,
            Instant closedAt,
            BigDecimal initialBalance,
            BigDecimal totalCollected,
            BigDecimal otherIncomeCollected,
            BigDecimal expensesTotal,
            Integer vehiclesServed,
            BigDecimal expectedCash,
            BigDecimal reportedCash,
            BigDecimal difference
    ) {
    }

    private final CashSessionRepository cashSessionRepository;
    private final PaymentRepository paymentRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TariffRepository tariffRepository;
    private final UserRepository userRepository;
    private final TimeProvider timeProvider;
    private final com.parking.domain.repository.SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final com.parking.domain.repository.SubscriptionRepository subscriptionRepository;
    private final CashTransactionService cashTransactionService;

    public CashOperationsService(CashSessionRepository cashSessionRepository, PaymentRepository paymentRepository,
                                 ParkingSessionRepository parkingSessionRepository,
                                 PaymentMethodRepository paymentMethodRepository, TariffRepository tariffRepository,
                                 UserRepository userRepository, TimeProvider timeProvider,
                                 com.parking.domain.repository.SubscriptionPaymentRepository subscriptionPaymentRepository,
                                 com.parking.domain.repository.SubscriptionRepository subscriptionRepository,
                                 CashTransactionService cashTransactionService) {
        this.cashTransactionService = cashTransactionService;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.paymentRepository = paymentRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.tariffRepository = tariffRepository;
        this.userRepository = userRepository;
        this.timeProvider = timeProvider;
    }

    /**
     * RF-059: el empleado consulta unicamente las operaciones de su propia caja; el administrador,
     * las de cualquiera. Mas recientes primero.
     */
    public List<CashOperation> operations(String cashSessionId, String actingUserId, boolean actingIsAdmin) {
        CashSession session = cashSessionRepository.findById(cashSessionId)
                .orElseThrow(() -> new NotFoundException("Caja no encontrada"));
        if (!actingIsAdmin && !session.employeeId().equals(actingUserId)) {
            throw new AccessDeniedException("Solo el empleado dueño de la caja o un administrador puede ver sus operaciones");
        }
        java.util.List<CashOperation> operations = new java.util.ArrayList<>(
                paymentRepository.findByCashSessionId(cashSessionId).stream().map(this::toOperation).toList());
        subscriptionPaymentRepository.findByCashSessionId(cashSessionId).forEach(p -> operations.add(new CashOperation(
                p.id(), p.operationNumber(), null, p.paidAt(), p.amount(),
                paymentMethodRepository.findById(p.paymentMethodId()).map(PaymentMethod::name).orElse(null),
                "Mensualidad - " + subscriptionRepository.findById(p.subscriptionId())
                        .map(com.parking.domain.model.Subscription::customerName).orElse("cliente"),
                "SUBSCRIPTION", "ACTIVE", null)));
        cashTransactionService.byCashSession(cashSessionId).forEach(t -> operations.add(new CashOperation(
                t.id(), t.operationNumber(), null, t.createdAt(), t.amount(), t.paymentMethodName(),
                t.description() != null ? t.conceptName() + " - " + t.description() : t.conceptName(),
                t.type(), t.status(), t.voidReason())));
        operations.sort(Comparator.comparing(CashOperation::paidAt).reversed());
        return operations;
    }

    /** RF-057: cajas cerradas en un rango (fecha de cierre, zona del parqueadero), mas recientes primero. */
    public List<ClosedCashSession> closedBetween(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        ZoneId zone = timeProvider.zone();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);
        return cashSessionRepository.findClosedByClosedAtBetween(start, end).stream()
                .sorted(Comparator.comparing(CashSession::closedAt).reversed())
                .map(c -> new ClosedCashSession(c.id(), c.employeeId(),
                        userRepository.findById(c.employeeId()).map(User::fullName).orElse(null),
                        c.openedAt(), c.closedAt(), c.initialBalance(), c.totalCollected(),
                        c.otherIncomeCollected(), c.expensesTotal(), c.vehiclesServed(),
                        c.expectedCash(), c.reportedCash(), c.difference()))
                .toList();
    }

    private CashOperation toOperation(Payment payment) {
        ParkingSession session = parkingSessionRepository.findById(payment.parkingSessionId()).orElse(null);
        String tariffName = session != null && session.appliedTariffId() != null
                ? tariffRepository.findById(session.appliedTariffId()).map(Tariff::name).orElse(null) : null;
        return new CashOperation(payment.id(), payment.operationNumber(),
                session != null ? session.plate() : null, payment.paidAt(), payment.chargedAmount(),
                paymentMethodRepository.findById(payment.paymentMethodId()).map(PaymentMethod::name).orElse(null),
                tariffName, "PARKING", "ACTIVE", null);
    }
}
