package com.parking.application.parking;

import com.parking.application.audit.AuditService;
import com.parking.domain.exception.BusinessRuleViolationException;
import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.CashMovement;
import com.parking.domain.model.CashMovementType;
import com.parking.domain.model.CashSession;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.ParkingStatus;
import com.parking.domain.model.Payment;
import com.parking.domain.model.PaymentMethod;
import com.parking.domain.repository.CashMovementRepository;
import com.parking.domain.repository.CashSessionRepository;
import com.parking.domain.repository.ParkingSessionRepository;
import com.parking.domain.repository.PaymentMethodRepository;
import com.parking.domain.repository.PaymentRepository;
import com.parking.domain.tariffengine.TariffCalculationResult;
import com.parking.domain.tariffengine.TariffCalculationService;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** UC-004 y UC-005 (RF-034 a RF-051). */
@Service
public class ParkingExitService {

    private final ParkingSessionRepository parkingSessionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final TariffCalculationService tariffCalculationService;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public ParkingExitService(ParkingSessionRepository parkingSessionRepository, PaymentRepository paymentRepository,
                               PaymentMethodRepository paymentMethodRepository,
                               CashSessionRepository cashSessionRepository,
                               CashMovementRepository cashMovementRepository,
                               TariffCalculationService tariffCalculationService, AuditService auditService,
                               TimeProvider timeProvider) {
        this.parkingSessionRepository = parkingSessionRepository;
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.tariffCalculationService = tariffCalculationService;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    /**
     * UC-005: calcula sin registrar (pantalla de cobro/salida, antes de confirmar). Devuelve todas
     * las tarifas aplicables ordenadas por BR-008: la primera es la sugerida, el resto las alternas
     * (BR-008.1).
     */
    public List<TariffCalculationResult> quote(String sessionId) {
        ParkingSession session = getActiveSessionOrThrow(sessionId);
        return tariffCalculationService.options(session, timeProvider.now());
    }

    /**
     * @param tariffId tarifa elegida al cobrar (BR-008.1); null = la sugerida por el motor. Debe ser
     *                 una de las aplicables a la sesion; el valor lo recalcula siempre el motor.
     */
    @Transactional
    public CheckoutResult checkout(String sessionId, String paymentMethodId, BigDecimal chargedAmount,
                                    String tariffId, String employeeId) {
        ParkingSession session = getActiveSessionOrThrow(sessionId);

        PaymentMethod method = paymentMethodRepository.findById(paymentMethodId)
                .filter(PaymentMethod::active)
                .orElseThrow(() -> new NotFoundException("Medio de pago no encontrado o inactivo"));

        CashSession cashSession = cashSessionRepository.findOpenByEmployeeId(employeeId)
                .orElseThrow(() -> new BusinessRuleViolationException("NO_OPEN_CASH_SESSION",
                        "Debe abrir caja antes de registrar cobros"));

        // BR-021: barrera de aplicacion; la constraint unica en BD sobre parking_session_id es la
        // barrera definitiva ante condiciones de carrera (RNF-019).
        if (paymentRepository.findByParkingSessionId(sessionId).isPresent()) {
            throw new BusinessRuleViolationException("ALREADY_PAID", "Esta sesión ya tiene un pago registrado");
        }

        Instant exitDateTime = timeProvider.now();
        TariffCalculationResult suggested = tariffCalculationService.calculate(session, exitDateTime);
        TariffCalculationResult tariffResult = tariffCalculationService.calculateWith(session, exitDateTime, tariffId);

        // RF-037 / BR-008.1: el valor cobrado debe ser exactamente el calculado por el motor. Tambien
        // detecta una cotizacion desactualizada (paso una fraccion de tiempo entre cotizar y confirmar):
        // el cliente debe volver a cotizar y confirmar el valor nuevo.
        if (chargedAmount.compareTo(tariffResult.amount()) != 0) {
            throw new BusinessRuleViolationException("CHARGED_AMOUNT_MISMATCH",
                    "El valor a cobrar cambió a $" + tariffResult.amount().toPlainString()
                            + ". Revisa el nuevo valor y confirma de nuevo.");
        }

        ParkingSession closedSession = new ParkingSession(
                session.id(), session.plate(), session.vehicleTypeId(), session.entryDateTime(), exitDateTime,
                ParkingStatus.FINALIZADO, session.entryEmployeeId(), employeeId, tariffResult.tariffId(),
                tariffResult.amount(), session.notes(), null, null, null, session.createdAt(), exitDateTime);
        ParkingSession savedSession = parkingSessionRepository.save(closedSession);

        // Antes: count()+1, que con dos cobros simultaneos en MySQL generaba el mismo numero y el
        // segundo fallaba con 500 (restriccion unica). Ahora el consecutivo se reserva atomicamente.
        String operationNumber = "OP-" + String.format("%06d", paymentRepository.nextOperationSequence());
        Payment payment = new Payment(UUID.randomUUID().toString(), sessionId, cashSession.id(),
                tariffResult.amount(), chargedAmount, paymentMethodId, operationNumber, employeeId,
                exitDateTime, exitDateTime);
        Payment savedPayment = paymentRepository.save(payment);

        cashMovementRepository.save(new CashMovement(UUID.randomUUID().toString(), cashSession.id(),
                savedPayment.id(), CashMovementType.PAYMENT, chargedAmount, null, exitDateTime));

        // BR-008.1 + BR-025: cambiar la tarifa sugerida es una operacion sensible -> auditoria con la
        // tarifa sugerida (valor anterior) y la elegida (valor nuevo).
        if (!suggested.tariffId().equals(tariffResult.tariffId())) {
            auditService.record(employeeId, "TARIFF_OVERRIDDEN", "ParkingSession", sessionId,
                    tariffJson(suggested), tariffJson(tariffResult));
        }

        return new CheckoutResult(savedSession, tariffResult, savedPayment, method);
    }

    /** UC-011 (RF-068). BR-020, BR-024 (soft delete, nunca se borra fisicamente), BR-025 (auditoría). */
    @Transactional
    public ParkingSession cancel(String sessionId, String reason, String adminUserId) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("El motivo de la anulación es obligatorio");
        }
        ParkingSession session = parkingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Sesión de parqueo no encontrada"));
        if (session.status() != ParkingStatus.EN_PARQUEADERO) {
            throw new BusinessRuleViolationException("SESSION_NOT_ACTIVE",
                    "Solo se pueden anular sesiones activas (sin cobro registrado)");
        }

        Instant now = timeProvider.now();
        ParkingSession cancelled = new ParkingSession(session.id(), session.plate(), session.vehicleTypeId(),
                session.entryDateTime(), session.exitDateTime(), ParkingStatus.ANULADO, session.entryEmployeeId(),
                session.exitEmployeeId(), session.appliedTariffId(), session.calculatedAmount(), session.notes(),
                reason, adminUserId, now, session.createdAt(), now);
        ParkingSession saved = parkingSessionRepository.save(cancelled);

        auditService.record(adminUserId, "PARKING_SESSION_CANCELLED", "ParkingSession", sessionId,
                "{\"status\":\"EN_PARQUEADERO\"}",
                "{\"status\":\"ANULADO\",\"reason\":\"" + reason.replace("\"", "'") + "\"}");
        return saved;
    }

    private static String tariffJson(TariffCalculationResult result) {
        return "{\"tariffId\":\"" + result.tariffId() + "\",\"tariffName\":\""
                + result.tariffName().replace("\"", "'") + "\",\"amount\":" + result.amount().toPlainString() + "}";
    }

    private ParkingSession getActiveSessionOrThrow(String sessionId) {
        ParkingSession session = parkingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Sesión de parqueo no encontrada"));
        if (session.status() != ParkingStatus.EN_PARQUEADERO) {
            throw new BusinessRuleViolationException("SESSION_NOT_ACTIVE",
                    "La sesión ya fue finalizada o anulada");
        }
        return session;
    }
}
