package com.parking.application.cash;

import com.parking.application.audit.AuditService;
import com.parking.domain.exception.BusinessRuleViolationException;
import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.CashMovement;
import com.parking.domain.model.CashMovementType;
import com.parking.domain.model.CashSession;
import com.parking.domain.model.CashSessionStatus;
import com.parking.domain.model.Payment;
import com.parking.domain.model.PaymentMethod;
import com.parking.domain.model.PaymentMethodCategory;
import com.parking.domain.repository.CashMovementRepository;
import com.parking.domain.repository.CashSessionRepository;
import com.parking.domain.repository.PaymentMethodRepository;
import com.parking.domain.repository.PaymentRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** UC-006 (apertura, Fase 5) y UC-007 (cierre/arqueo, Fase 6). */
@Service
public class CashSessionService {

    private final CashSessionRepository cashSessionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final CashMovementRepository cashMovementRepository;
    private final AuditService auditService;
    private final TimeProvider timeProvider;
    private final com.parking.domain.repository.SubscriptionPaymentRepository subscriptionPaymentRepository;

    public CashSessionService(CashSessionRepository cashSessionRepository, PaymentRepository paymentRepository,
                               PaymentMethodRepository paymentMethodRepository,
                               CashMovementRepository cashMovementRepository, AuditService auditService,
                               TimeProvider timeProvider, com.parking.domain.repository.SubscriptionPaymentRepository subscriptionPaymentRepository) {
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public CashSession open(String employeeId, BigDecimal initialBalance) {
        cashSessionRepository.findOpenByEmployeeId(employeeId).ifPresent(existing -> {
            throw new BusinessRuleViolationException("CASH_SESSION_ALREADY_OPEN",
                    "Ya tiene una caja abierta");
        });

        Instant now = timeProvider.now();
        CashSession session = new CashSession(UUID.randomUUID().toString(), employeeId, CashSessionStatus.OPEN,
                now, initialBalance, null, null, null, null, null, null, null, null, null, now, now);
        return cashSessionRepository.save(session);
    }

    public CashSession getCurrentOpen(String employeeId) {
        return cashSessionRepository.findOpenByEmployeeId(employeeId)
                .orElseThrow(() -> new NotFoundException("No tiene una caja abierta"));
    }

    /** RF-055: totales en vivo, usados tanto para la previsualizacion como para el cierre real. */
    public CashSessionSummary summarize(CashSession session) {
        List<Payment> payments = paymentRepository.findByCashSessionId(session.id());

        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal electronic = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;

        for (Payment payment : payments) {
            PaymentMethodCategory category = paymentMethodRepository.findById(payment.paymentMethodId())
                    .map(PaymentMethod::category)
                    .orElse(PaymentMethodCategory.OTHER);
            switch (category) {
                case CASH -> cash = cash.add(payment.chargedAmount());
                case ELECTRONIC -> electronic = electronic.add(payment.chargedAmount());
                case OTHER -> other = other.add(payment.chargedAmount());
            }
        }

        // BR-031: las mensualidades cobradas en esta caja suman igual que los cobros de salida (arqueo).
        for (var subscriptionPayment : subscriptionPaymentRepository.findByCashSessionId(session.id())) {
            PaymentMethodCategory category = paymentMethodRepository.findById(subscriptionPayment.paymentMethodId())
                    .map(PaymentMethod::category)
                    .orElse(PaymentMethodCategory.OTHER);
            switch (category) {
                case CASH -> cash = cash.add(subscriptionPayment.amount());
                case ELECTRONIC -> electronic = electronic.add(subscriptionPayment.amount());
                case OTHER -> other = other.add(subscriptionPayment.amount());
            }
        }

        BigDecimal total = cash.add(electronic).add(other);
        // BR-022/RF-055: solo el efectivo afecta lo que fisicamente debe haber en el cajon.
        BigDecimal expectedCash = session.initialBalance().add(cash);

        return new CashSessionSummary(session.initialBalance(), cash, electronic, other, total,
                payments.size(), expectedCash);
    }

    /**
     * Resumen de una caja: solo su dueno o un administrador (revision OWASP A01, 2026-09-29; antes
     * cualquier empleado podia leer los totales de la caja de otro).
     */
    public CashSessionSummary summarize(String cashSessionId, String actingEmployeeId, boolean actingIsAdmin) {
        CashSession session = cashSessionRepository.findById(cashSessionId)
                .orElseThrow(() -> new NotFoundException("Caja no encontrada"));
        if (!actingIsAdmin && !session.employeeId().equals(actingEmployeeId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo el empleado dueño de la caja o un administrador puede ver su resumen");
        }
        return summarize(session);
    }

    @Transactional
    public CashSession close(String cashSessionId, String actingEmployeeId, boolean actingIsAdmin,
                              BigDecimal reportedCash) {
        CashSession session = cashSessionRepository.findById(cashSessionId)
                .orElseThrow(() -> new NotFoundException("Caja no encontrada"));

        if (!actingIsAdmin && !session.employeeId().equals(actingEmployeeId)) {
            throw new BusinessRuleViolationException("NOT_CASH_SESSION_OWNER",
                    "Solo el empleado que abrió la caja puede cerrarla");
        }
        if (session.status() != CashSessionStatus.OPEN) {
            throw new BusinessRuleViolationException("CASH_SESSION_ALREADY_CLOSED", "Esta caja ya fue cerrada");
        }

        CashSessionSummary summary = summarize(session);
        Instant now = timeProvider.now();
        BigDecimal difference = reportedCash.subtract(summary.expectedCash());

        CashSession closed = new CashSession(session.id(), session.employeeId(), CashSessionStatus.CLOSED,
                session.openedAt(), session.initialBalance(), now, summary.cashCollected(),
                summary.electronicCollected(), summary.otherCollected(), summary.totalCollected(),
                summary.vehiclesServed(), summary.expectedCash(), reportedCash, difference,
                session.createdAt(), now);

        CashSession saved = cashSessionRepository.save(closed);
        // RF-066 / BR-025: el cierre de caja queda en auditoria (antes no se registraba).
        auditService.record(actingEmployeeId, "CASH_SESSION_CLOSED", "CashSession", session.id(),
                "{\"status\":\"OPEN\"}",
                "{\"status\":\"CLOSED\",\"totalCollected\":" + summary.totalCollected().toPlainString()
                        + ",\"expectedCash\":" + summary.expectedCash().toPlainString()
                        + ",\"reportedCash\":" + reportedCash.toPlainString()
                        + ",\"difference\":" + difference.toPlainString()
                        + ",\"vehiclesServed\":" + summary.vehiclesServed() + "}");
        return saved;
    }

    /**
     * RF-057/BR-023: correccion administrativa de un cierre ya finalizado, con auditoria
     * obligatoria (BR-025). Unicamente ajusta el efectivo reportado (y por tanto la diferencia) —
     * los totales calculados (cash/electronic/other/total/vehiculos) no cambian, ya que reflejan
     * transacciones ya ocurridas.
     */
    @Transactional
    public CashSession adjustReportedCash(String cashSessionId, BigDecimal newReportedCash, String reason,
                                           String adminUserId) {
        CashSession session = cashSessionRepository.findById(cashSessionId)
                .orElseThrow(() -> new NotFoundException("Caja no encontrada"));
        if (session.status() != CashSessionStatus.CLOSED) {
            throw new BusinessRuleViolationException("CASH_SESSION_NOT_CLOSED",
                    "Solo se pueden corregir cajas ya cerradas");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("El motivo de la corrección es obligatorio");
        }

        BigDecimal oldReportedCash = session.reportedCash();
        BigDecimal newDifference = newReportedCash.subtract(session.expectedCash());
        Instant now = timeProvider.now();

        CashSession updated = new CashSession(session.id(), session.employeeId(), session.status(),
                session.openedAt(), session.initialBalance(), session.closedAt(), session.cashCollected(),
                session.electronicCollected(), session.otherCollected(), session.totalCollected(),
                session.vehiclesServed(), session.expectedCash(), newReportedCash, newDifference,
                session.createdAt(), now);
        CashSession saved = cashSessionRepository.save(updated);

        auditService.record(adminUserId, "CASH_SESSION_ADJUSTED", "CashSession", cashSessionId,
                "{\"reportedCash\":" + oldReportedCash + "}",
                "{\"reportedCash\":" + newReportedCash + ",\"reason\":\"" + reason.replace("\"", "'") + "\"}");

        cashMovementRepository.save(new CashMovement(UUID.randomUUID().toString(), cashSessionId, null,
                CashMovementType.ADJUSTMENT, newReportedCash.subtract(oldReportedCash), reason, now));

        return saved;
    }
}
