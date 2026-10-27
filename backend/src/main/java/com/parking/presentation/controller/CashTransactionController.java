package com.parking.presentation.controller;

import com.parking.application.cash.CashConceptService;
import com.parking.application.cash.CashTransactionService;
import com.parking.application.cash.CashTransactionService.CashTransactionReceipt;
import com.parking.application.cash.CashTransactionService.CashTransactionView;
import com.parking.domain.model.CashConcept;
import com.parking.domain.model.CashTransactionType;
import com.parking.infrastructure.security.ParkingUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * UC-014 (RF-079 a RF-082): conceptos, otros ingresos y gastos de caja. Registrar un ingreso: cualquier
 * rol; un gasto, anular y editar conceptos: solo administrador.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
public class CashTransactionController {

    private final CashConceptService conceptService;
    private final CashTransactionService transactionService;

    public CashTransactionController(CashConceptService conceptService, CashTransactionService transactionService) {
        this.conceptService = conceptService;
        this.transactionService = transactionService;
    }

    public record ConceptRequest(@NotNull(message = "el tipo es obligatorio") CashTransactionType type,
                                 @NotBlank(message = "el nombre es obligatorio") String name) {
    }

    public record ConceptRenameRequest(@NotBlank(message = "el nombre es obligatorio") String name) {
    }

    public record ConceptStatusRequest(@NotNull(message = "el estado es obligatorio") Boolean active) {
    }

    public record TransactionRequest(
            @NotNull(message = "el tipo es obligatorio") CashTransactionType type,
            @NotBlank(message = "el concepto es obligatorio") String conceptId,
            String description,
            @NotNull(message = "el valor es obligatorio") BigDecimal amount,
            @NotBlank(message = "el medio de pago es obligatorio") String paymentMethodId,
            /** Solo para gastos: caja abierta de la que sale el dinero. */
            String cashSessionId) {
    }

    public record VoidRequest(@NotBlank(message = "el motivo es obligatorio") String reason) {
    }

    @GetMapping("/cash-concepts")
    public List<CashConcept> concepts(@RequestParam(required = false) CashTransactionType type,
                                      @RequestParam(required = false) Boolean active) {
        return conceptService.list(type, active);
    }

    @PostMapping("/cash-concepts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CashConcept createConcept(@Valid @RequestBody ConceptRequest request,
                                     @AuthenticationPrincipal ParkingUserPrincipal principal) {
        return conceptService.create(request.type(), request.name(), principal.id());
    }

    @PutMapping("/cash-concepts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CashConcept renameConcept(@PathVariable String id, @Valid @RequestBody ConceptRenameRequest request,
                                     @AuthenticationPrincipal ParkingUserPrincipal principal) {
        return conceptService.rename(id, request.name(), principal.id());
    }

    @PatchMapping("/cash-concepts/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public CashConcept conceptStatus(@PathVariable String id, @Valid @RequestBody ConceptStatusRequest request,
                                     @AuthenticationPrincipal ParkingUserPrincipal principal) {
        return conceptService.setActive(id, request.active(), principal.id());
    }

    /** BR-032: un EXPENSE de un usuario que no es administrador responde 403. */
    @PostMapping("/cash-transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public CashTransactionView register(@Valid @RequestBody TransactionRequest request,
                                        @AuthenticationPrincipal ParkingUserPrincipal principal) {
        return transactionService.view(transactionService.register(request.type(), request.conceptId(),
                request.description(), request.amount(), request.paymentMethodId(), request.cashSessionId(),
                principal.id(), isAdmin(principal)));
    }

    @PostMapping("/cash-transactions/{id}/void")
    @PreAuthorize("hasRole('ADMIN')")
    public CashTransactionView voidTransaction(@PathVariable String id, @Valid @RequestBody VoidRequest request,
                                               @AuthenticationPrincipal ParkingUserPrincipal principal) {
        return transactionService.view(transactionService.voidTransaction(id, request.reason(), principal.id()));
    }

    @GetMapping("/cash-transactions/{id}/receipt")
    public CashTransactionReceipt receipt(@PathVariable String id,
                                          @AuthenticationPrincipal ParkingUserPrincipal principal) {
        return transactionService.receipt(id, principal.id(), isAdmin(principal));
    }

    /** RF-081: cajas abiertas de las que el administrador puede registrar un gasto. */
    @GetMapping("/cash-sessions/open")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CashTransactionService.OpenCashSession> openSessions() {
        return transactionService.openSessions();
    }

    private static boolean isAdmin(ParkingUserPrincipal principal) {
        return "ADMIN".equals(principal.roleCode());
    }
}
