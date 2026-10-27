package com.parking.application.cash;

import com.parking.application.audit.AuditService;
import com.parking.domain.exception.BusinessRuleViolationException;
import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.CashConcept;
import com.parking.domain.model.CashSession;
import com.parking.domain.model.CashSessionStatus;
import com.parking.domain.model.CashTransaction;
import com.parking.domain.model.CashTransactionStatus;
import com.parking.domain.model.CashTransactionType;
import com.parking.domain.model.Configuration;
import com.parking.domain.model.PaymentMethod;
import com.parking.domain.model.PaymentMethodCategory;
import com.parking.domain.model.User;
import com.parking.domain.repository.CashSessionRepository;
import com.parking.domain.repository.CashTransactionRepository;
import com.parking.domain.repository.ConfigurationRepository;
import com.parking.domain.repository.PaymentMethodRepository;
import com.parking.domain.repository.PaymentRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.parking.application.audit.AuditJson.str;

/**
 * UC-014: otros ingresos (cualquier rol, en su caja abierta) y gastos de caja (solo administrador, en
 * una caja abierta que elige). BR-032 a BR-035.
 */
@Service
public class CashTransactionService {

    /** Movimiento con los nombres resueltos, para la caja, los reportes y el comprobante. */
    public record CashTransactionView(
            String id,
            String type,
            String conceptId,
            String conceptName,
            String description,
            BigDecimal amount,
            String paymentMethodName,
            String operationNumber,
            String userName,
            String cashSessionId,
            String cashierName,
            Instant createdAt,
            String status,
            String voidReason
    ) {
    }

    /** RF-082: comprobante imprimible de un ingreso o un gasto. */
    public record CashTransactionReceipt(String parkingName, String nit, String address, String phone,
                                         CashTransactionView transaction) {
    }

    /** Caja abierta que el administrador puede elegir para un gasto (RF-081). */
    public record OpenCashSession(String id, String employeeId, String employeeName, Instant openedAt,
                                  BigDecimal expectedCash) {
    }

    private final CashTransactionRepository transactionRepository;
    private final CashConceptService conceptService;
    private final CashSessionRepository cashSessionRepository;
    private final CashSessionService cashSessionService;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ConfigurationRepository configurationRepository;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public CashTransactionService(CashTransactionRepository transactionRepository, CashConceptService conceptService,
                                  CashSessionRepository cashSessionRepository, CashSessionService cashSessionService,
                                  PaymentMethodRepository paymentMethodRepository, PaymentRepository paymentRepository,
                                  UserRepository userRepository, ConfigurationRepository configurationRepository,
                                  AuditService auditService, TimeProvider timeProvider) {
        this.transactionRepository = transactionRepository;
        this.conceptService = conceptService;
        this.cashSessionRepository = cashSessionRepository;
        this.cashSessionService = cashSessionService;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.configurationRepository = configurationRepository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    /**
     * BR-032: un ingreso va a la caja abierta de quien lo registra; un gasto, solo del administrador,
     * a la caja abierta indicada en cashSessionId.
     */
    @Transactional
    public CashTransaction register(CashTransactionType type, String conceptId, String description, BigDecimal amount,
                                    String paymentMethodId, String cashSessionId, String actorId, boolean actorIsAdmin) {
        if (type == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio");
        }
        if (type == CashTransactionType.EXPENSE && !actorIsAdmin) {
            throw new AccessDeniedException("Solo el administrador puede registrar gastos de caja");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("El valor debe ser mayor que cero");
        }
        CashConcept concept = conceptService.find(conceptId);
        if (concept.type() != type || !concept.active()) {
            throw new IllegalArgumentException("El concepto no está activo o no corresponde al tipo de movimiento");
        }
        PaymentMethod method = paymentMethodRepository.findById(paymentMethodId).filter(PaymentMethod::active)
                .orElseThrow(() -> new NotFoundException("Medio de pago no encontrado o inactivo"));
        String cleanDescription = cleanDescription(description);

        CashSession session;
        if (type == CashTransactionType.INCOME) {
            session = cashSessionRepository.findOpenByEmployeeId(actorId)
                    .orElseThrow(() -> new BusinessRuleViolationException("NO_OPEN_CASH_SESSION",
                            "Debe abrir caja antes de registrar ingresos"));
        } else {
            if (cashSessionId == null || cashSessionId.isBlank()) {
                throw new IllegalArgumentException("Seleccione la caja de la que sale el gasto");
            }
            session = cashSessionRepository.findById(cashSessionId)
                    .filter(s -> s.status() == CashSessionStatus.OPEN)
                    .orElseThrow(() -> new BusinessRuleViolationException("CASH_SESSION_NOT_OPEN",
                            "La caja seleccionada no está abierta"));
            // BR-033: no puede salir del cajon mas efectivo del que deberia haber.
            if (method.category() == PaymentMethodCategory.CASH) {
                BigDecimal available = cashSessionService.summarize(session).expectedCash();
                if (amount.compareTo(available) > 0) {
                    throw new BusinessRuleViolationException("INSUFFICIENT_CASH",
                            "El gasto supera el efectivo disponible en la caja ($" + available.toPlainString() + ")");
                }
            }
        }

        // BR-034: mismo consecutivo OP- que los cobros.
        String operationNumber = "OP-" + String.format("%06d", paymentRepository.nextOperationSequence());
        CashTransaction saved = transactionRepository.save(new CashTransaction(UUID.randomUUID().toString(),
                session.id(), type, concept.id(), cleanDescription, amount, method.id(), operationNumber, actorId,
                timeProvider.now(), CashTransactionStatus.ACTIVE, null, null, null));

        auditService.record(actorId,
                type == CashTransactionType.EXPENSE ? "CASH_EXPENSE_REGISTERED" : "CASH_INCOME_REGISTERED",
                "CashTransaction", saved.id(), null, describe(saved, concept.name()));
        return saved;
    }

    /** BR-035: solo el administrador, con motivo y mientras la caja sigue abierta. */
    @Transactional
    public CashTransaction voidTransaction(String id, String reason, String adminId) {
        CashTransaction existing = find(id);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("El motivo de la anulación es obligatorio");
        }
        if (!existing.isActive()) {
            throw new BusinessRuleViolationException("CASH_TRANSACTION_ALREADY_VOIDED", "El movimiento ya fue anulado");
        }
        CashSession session = cashSessionRepository.findById(existing.cashSessionId())
                .orElseThrow(() -> new NotFoundException("Caja no encontrada"));
        if (session.status() != CashSessionStatus.OPEN) {
            throw new BusinessRuleViolationException("CASH_SESSION_CLOSED",
                    "No se puede anular: la caja del movimiento ya fue cerrada");
        }
        String cleanReason = reason.trim().length() > 255 ? reason.trim().substring(0, 255) : reason.trim();
        CashTransaction voided = transactionRepository.save(new CashTransaction(existing.id(), existing.cashSessionId(),
                existing.type(), existing.conceptId(), existing.description(), existing.amount(),
                existing.paymentMethodId(), existing.operationNumber(), existing.userId(), existing.createdAt(),
                CashTransactionStatus.VOIDED, cleanReason, adminId, timeProvider.now()));
        String conceptName = conceptService.find(existing.conceptId()).name();
        auditService.record(adminId, "CASH_TRANSACTION_VOIDED", "CashTransaction", id,
                describe(existing, conceptName), describe(voided, conceptName));
        return voided;
    }

    /** RF-082: lo ve el administrador, quien lo registro o el dueno de la caja. */
    public CashTransactionReceipt receipt(String id, String actorId, boolean actorIsAdmin) {
        CashTransaction transaction = find(id);
        if (!actorIsAdmin && !transaction.userId().equals(actorId)) {
            boolean owner = cashSessionRepository.findById(transaction.cashSessionId())
                    .map(s -> s.employeeId().equals(actorId)).orElse(false);
            if (!owner) {
                throw new AccessDeniedException("Solo el administrador o el dueño de la caja pueden ver este comprobante");
            }
        }
        Configuration configuration = configurationRepository.find().orElse(null);
        return new CashTransactionReceipt(
                configuration != null ? configuration.parkingName() : null,
                configuration != null ? configuration.nit() : null,
                configuration != null ? configuration.address() : null,
                configuration != null ? configuration.phone() : null,
                view(transaction));
    }

    /** Movimientos de una caja (incluye anulados). */
    public List<CashTransactionView> byCashSession(String cashSessionId) {
        return transactionRepository.findByCashSessionId(cashSessionId).stream().map(this::view).toList();
    }

    /** RF-083: movimientos del rango (incluye anulados), mas recientes primero. */
    public List<CashTransactionView> between(Instant from, Instant to) {
        return transactionRepository.findByCreatedAtBetween(from, to).stream()
                .sorted(Comparator.comparing(CashTransaction::createdAt).reversed())
                .map(this::view).toList();
    }

    /** RF-081: cajas abiertas con su efectivo esperado actual (solo administrador). */
    public List<OpenCashSession> openSessions() {
        return cashSessionRepository.findOpen().stream()
                .sorted(Comparator.comparing(CashSession::openedAt))
                .map(s -> new OpenCashSession(s.id(), s.employeeId(), userName(s.employeeId()), s.openedAt(),
                        cashSessionService.summarize(s).expectedCash()))
                .toList();
    }

    public CashTransactionView view(CashTransaction t) {
        String conceptName = conceptService.find(t.conceptId()).name();
        String methodName = paymentMethodRepository.findById(t.paymentMethodId()).map(PaymentMethod::name).orElse(null);
        String cashierName = cashSessionRepository.findById(t.cashSessionId())
                .map(s -> userName(s.employeeId())).orElse(null);
        return new CashTransactionView(t.id(), t.type().name(), t.conceptId(), conceptName, t.description(), t.amount(),
                methodName, t.operationNumber(), userName(t.userId()), t.cashSessionId(), cashierName, t.createdAt(),
                t.status().name(), t.voidReason());
    }

    private CashTransaction find(String id) {
        return transactionRepository.findById(id).orElseThrow(() -> new NotFoundException("Movimiento no encontrado"));
    }

    private String userName(String userId) {
        return userRepository.findById(userId).map(User::fullName).orElse(null);
    }

    private static String cleanDescription(String description) {
        if (description == null || description.isBlank()) return null;
        String clean = description.trim();
        if (clean.length() > 255) {
            throw new IllegalArgumentException("La descripción admite máximo 255 caracteres");
        }
        return clean;
    }

    private static String describe(CashTransaction t, String conceptName) {
        return "{\"type\":" + str(t.type().name()) + ",\"concept\":" + str(conceptName)
                + ",\"amount\":" + t.amount().toPlainString() + ",\"operationNumber\":" + str(t.operationNumber())
                + ",\"cashSessionId\":" + str(t.cashSessionId()) + ",\"status\":" + str(t.status().name())
                + (t.voidReason() != null ? ",\"voidReason\":" + str(t.voidReason()) : "") + "}";
    }
}
