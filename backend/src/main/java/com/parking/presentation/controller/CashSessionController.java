package com.parking.presentation.controller;

import com.parking.application.cash.CashOperationsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.util.List;
import com.parking.application.cash.CashSessionService;
import com.parking.domain.model.CashSession;
import com.parking.infrastructure.security.ParkingUserPrincipal;
import com.parking.presentation.dto.CashSessionAdjustmentRequest;
import com.parking.presentation.dto.CashSessionCloseRequest;
import com.parking.presentation.dto.CashSessionOpenRequest;
import com.parking.presentation.dto.CashSessionResponse;
import com.parking.presentation.dto.CashSessionSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** UC-006 (apertura, Fase 5) y UC-007 (cierre/arqueo, Fase 6). Ver 08-api-contract.md §Caja. */
@RestController
@RequestMapping("/api/cash-sessions")
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
public class CashSessionController {

    private final CashSessionService cashSessionService;
    private final CashOperationsService cashOperationsService;

    public CashSessionController(CashSessionService cashSessionService, CashOperationsService cashOperationsService) {
        this.cashSessionService = cashSessionService;
        this.cashOperationsService = cashOperationsService;
    }

    /** RF-057: cajas cerradas en un rango de fechas de cierre (solo Administrador), para corregirlas. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CashOperationsService.ClosedCashSession> closed(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return cashOperationsService.closedBetween(from, to);
    }

    /** RF-059: cobros de una caja; el empleado solo los de la suya (403 si es ajena). */
    @GetMapping("/{id}/payments")
    public List<CashOperationsService.CashOperation> operations(@PathVariable String id,
                                                                @AuthenticationPrincipal ParkingUserPrincipal principal) {
        return cashOperationsService.operations(id, principal.id(), "ADMIN".equals(principal.roleCode()));
    }

    @GetMapping("/current")
    public CashSessionResponse current(@AuthenticationPrincipal ParkingUserPrincipal principal) {
        return CashSessionResponse.from(cashSessionService.getCurrentOpen(principal.id()));
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public CashSessionResponse open(@Valid @RequestBody CashSessionOpenRequest request,
                                     @AuthenticationPrincipal ParkingUserPrincipal principal) {
        CashSession session = cashSessionService.open(principal.id(), request.initialBalance());
        return CashSessionResponse.from(session);
    }

    @GetMapping("/{id}/summary")
    public CashSessionSummaryResponse summary(@PathVariable String id,
                                              @AuthenticationPrincipal ParkingUserPrincipal principal) {
        boolean isAdmin = "ADMIN".equals(principal.roleCode());
        return CashSessionSummaryResponse.from(cashSessionService.summarize(id, principal.id(), isAdmin));
    }

    @PostMapping("/{id}/close")
    public CashSessionResponse close(@PathVariable String id, @Valid @RequestBody CashSessionCloseRequest request,
                                      @AuthenticationPrincipal ParkingUserPrincipal principal) {
        boolean isAdmin = "ADMIN".equals(principal.roleCode());
        CashSession session = cashSessionService.close(id, principal.id(), isAdmin, request.reportedCash());
        return CashSessionResponse.from(session);
    }

    @PostMapping("/{id}/adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    public CashSessionResponse adjust(@PathVariable String id, @Valid @RequestBody CashSessionAdjustmentRequest request,
                                       @AuthenticationPrincipal ParkingUserPrincipal principal) {
        CashSession session = cashSessionService.adjustReportedCash(
                id, request.reportedCash(), request.reason(), principal.id());
        return CashSessionResponse.from(session);
    }
}
