package com.parking.presentation.controller;

import com.parking.application.parking.CheckoutResult;
import com.parking.application.parking.ParkingEntryService;
import com.parking.application.parking.EntryReceiptService;
import com.parking.application.parking.ParkingHistoryService;
import com.parking.application.parking.ParkingExitService;
import com.parking.application.parking.ParkingSessionQueryService;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.ParkingStatus;
import org.springframework.format.annotation.DateTimeFormat;
import com.parking.infrastructure.security.ParkingUserPrincipal;
import com.parking.presentation.dto.CancelSessionRequest;
import com.parking.presentation.dto.CheckoutRequest;
import com.parking.presentation.dto.CheckoutResultResponse;
import com.parking.presentation.dto.ParkingEntryRequest;
import com.parking.presentation.dto.ParkingSessionResponse;
import com.parking.presentation.dto.TariffQuoteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** UC-002 a UC-005. Ver 08-api-contract.md §Parqueo. */
@RestController
@RequestMapping("/api/parking-sessions")
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
public class ParkingSessionController {

    private final ParkingEntryService parkingEntryService;
    private final ParkingExitService parkingExitService;
    private final ParkingSessionQueryService parkingSessionQueryService;
    private final ParkingHistoryService parkingHistoryService;
    private final EntryReceiptService entryReceiptService;
    private final com.parking.application.subscription.SubscriptionService subscriptionService;

    public ParkingSessionController(ParkingEntryService parkingEntryService, ParkingExitService parkingExitService,
                                     ParkingSessionQueryService parkingSessionQueryService,
                                     ParkingHistoryService parkingHistoryService,
                                     EntryReceiptService entryReceiptService,
                                     com.parking.application.subscription.SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
        this.parkingEntryService = parkingEntryService;
        this.parkingExitService = parkingExitService;
        this.parkingSessionQueryService = parkingSessionQueryService;
        this.parkingHistoryService = parkingHistoryService;
        this.entryReceiptService = entryReceiptService;
    }

    /** RF-058: historial con filtros (solo Administrador). Rango por fecha de ingreso, inclusivo. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ParkingHistoryService.HistoryPage history(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String employeeId) {
        ParkingStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                parsedStatus = ParkingStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado inválido: use EN_PARQUEADERO, FINALIZADO o ANULADO");
            }
        }
        return parkingHistoryService.search(from, to, plate, parsedStatus,
                employeeId == null || employeeId.isBlank() ? null : employeeId);
    }

    @GetMapping("/active")
    public List<ParkingSessionResponse> listActive(@RequestParam(required = false) String plate) {
        return parkingSessionQueryService.listActive(plate).stream().map(ParkingSessionResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ParkingSessionResponse getById(@PathVariable String id) {
        return ParkingSessionResponse.from(parkingSessionQueryService.getById(id));
    }

    /** RF-072: datos del comprobante de ingreso (cualquier estado, para reimpresión). */
    @GetMapping("/{id}/entry-receipt")
    public EntryReceiptService.EntryReceiptData entryReceipt(@PathVariable String id) {
        return entryReceiptService.getBySessionId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParkingSessionResponse register(@Valid @RequestBody ParkingEntryRequest request,
                                            @AuthenticationPrincipal ParkingUserPrincipal principal) {
        var result = parkingEntryService.registerEntry(
                request.plate(), request.vehicleTypeId(), request.notes(), principal.id());
        return ParkingSessionResponse.from(parkingSessionQueryService.getById(result.session().id()))
                .withNotice(result.subscriptionNotice());
    }

    @PostMapping("/{id}/quote")
    public TariffQuoteResponse quote(@PathVariable String id) {
        ParkingSession session = parkingExitService.activeSession(id);
        var coverage = TariffQuoteResponse.SubscriptionCoverage.from(
                subscriptionService.coverageOf(session).orElse(null));
        // BR-030: cubierta por mensualidad -> no se calcula tarifa (puede no existir una aplicable).
        if (coverage != null && coverage.covered()) {
            return TariffQuoteResponse.covered(parkingSessionQueryService.getById(id).elapsedMinutes(), coverage);
        }
        return TariffQuoteResponse.from(parkingExitService.quote(id), coverage);
    }

    /** RF-076 / BR-030: salida de un cliente mensual al dia (sin cobro). */
    @PostMapping("/{id}/exit-subscription")
    public ParkingSessionResponse exitSubscription(@PathVariable String id,
                                                   @AuthenticationPrincipal ParkingUserPrincipal principal) {
        parkingExitService.exitCoveredBySubscription(id, principal.id());
        return ParkingSessionResponse.from(parkingSessionQueryService.getById(id));
    }

    @PostMapping("/{id}/checkout")
    public CheckoutResultResponse checkout(@PathVariable String id, @Valid @RequestBody CheckoutRequest request,
                                            @AuthenticationPrincipal ParkingUserPrincipal principal) {
        CheckoutResult result = parkingExitService.checkout(
                id, request.paymentMethodId(), request.chargedAmount(), request.tariffId(), principal.id());
        return CheckoutResultResponse.from(result);
    }

    /** UC-011 (RF-068). */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ParkingSessionResponse cancel(@PathVariable String id, @Valid @RequestBody CancelSessionRequest request,
                                          @AuthenticationPrincipal ParkingUserPrincipal principal) {
        parkingExitService.cancel(id, request.reason(), principal.id());
        return ParkingSessionResponse.from(parkingSessionQueryService.getById(id));
    }
}
