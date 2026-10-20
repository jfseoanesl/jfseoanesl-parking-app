package com.parking.presentation.controller;

import com.parking.application.subscription.SubscriptionService;
import com.parking.application.subscription.SubscriptionService.SubscriptionInput;
import com.parking.application.subscription.SubscriptionService.SubscriptionView;
import com.parking.application.subscription.SubscriptionService.VehicleInput;
import com.parking.domain.model.Subscription;
import com.parking.domain.model.SubscriptionPayment;
import com.parking.infrastructure.security.ParkingUserPrincipal;
import com.parking.presentation.dto.UpdateActiveStatusRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * RF-074 a RF-077: mensualidades. Consultar y cobrar: administrador y empleado (el cobro exige caja
 * abierta); crear, editar y activar/desactivar: solo administrador.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    public record VehicleRequest(@NotBlank(message = "la placa es obligatoria") String plate,
                                 @NotBlank(message = "el tipo de vehículo es obligatorio") String vehicleTypeId) {
    }

    public record SubscriptionRequest(
            @NotBlank(message = "el nombre del cliente es obligatorio") String customerName,
            String documentId,
            String phone,
            @NotNull(message = "el valor mensual es obligatorio") BigDecimal monthlyFee,
            @NotNull(message = "el día de corte es obligatorio") Integer cutOffDay,
            String notes,
            @NotNull(message = "registre al menos una placa") List<@Valid VehicleRequest> vehicles
    ) {
        SubscriptionInput toInput() {
            return new SubscriptionInput(customerName, documentId, phone, monthlyFee, cutOffDay, notes,
                    vehicles.stream().map(v -> new VehicleInput(v.plate(), v.vehicleTypeId())).toList());
        }
    }

    public record PaymentRequest(@NotBlank(message = "el medio de pago es obligatorio") String paymentMethodId,
                                 @NotNull(message = "el valor es obligatorio") BigDecimal amount) {
    }

    public record VehicleResponse(String plate, String vehicleTypeId) {
    }

    public record SubscriptionResponse(String id, String customerName, String documentId, String phone,
                                       BigDecimal monthlyFee, int cutOffDay, String paidUntil, boolean active,
                                       String notes, String status, String vehicleInsidePlate,
                                       List<VehicleResponse> vehicles) {
        static SubscriptionResponse from(SubscriptionView view) {
            Subscription s = view.subscription();
            return new SubscriptionResponse(s.id(), s.customerName(), s.documentId(), s.phone(), s.monthlyFee(),
                    s.cutOffDay(), s.paidUntil() != null ? s.paidUntil().toString() : null, s.active(), s.notes(),
                    view.status().name(), view.vehicleInsidePlate(),
                    s.vehicles().stream().map(v -> new VehicleResponse(v.plate(), v.vehicleTypeId())).toList());
        }
    }

    public record PaymentResponse(String id, String subscriptionId, String operationNumber, BigDecimal amount,
                                  String periodFrom, String periodTo, String paymentMethodId, Instant paidAt) {
        static PaymentResponse from(SubscriptionPayment p) {
            return new PaymentResponse(p.id(), p.subscriptionId(), p.operationNumber(), p.amount(),
                    p.periodFrom().toString(), p.periodTo().toString(), p.paymentMethodId(), p.paidAt());
        }
    }

    @GetMapping("/subscriptions")
    public List<SubscriptionResponse> list() {
        return subscriptionService.list().stream().map(SubscriptionResponse::from).toList();
    }

    @GetMapping("/subscriptions/{id}")
    public SubscriptionResponse get(@PathVariable String id) {
        return SubscriptionResponse.from(subscriptionService.get(id));
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponse create(@Valid @RequestBody SubscriptionRequest request,
                                       @AuthenticationPrincipal ParkingUserPrincipal principal) {
        Subscription created = subscriptionService.create(request.toInput(), principal.id());
        return SubscriptionResponse.from(subscriptionService.get(created.id()));
    }

    @PutMapping("/subscriptions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponse update(@PathVariable String id, @Valid @RequestBody SubscriptionRequest request,
                                       @AuthenticationPrincipal ParkingUserPrincipal principal) {
        subscriptionService.update(id, request.toInput(), principal.id());
        return SubscriptionResponse.from(subscriptionService.get(id));
    }

    @PatchMapping("/subscriptions/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponse setStatus(@PathVariable String id, @Valid @RequestBody UpdateActiveStatusRequest request,
                                          @AuthenticationPrincipal ParkingUserPrincipal principal) {
        subscriptionService.setActive(id, request.active(), principal.id());
        return SubscriptionResponse.from(subscriptionService.get(id));
    }

    /** BR-031: cobro mensual en la caja abierta de quien cobra. */
    @PostMapping("/subscriptions/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@PathVariable String id, @Valid @RequestBody PaymentRequest request,
                               @AuthenticationPrincipal ParkingUserPrincipal principal) {
        return PaymentResponse.from(subscriptionService.registerPayment(
                id, request.paymentMethodId(), request.amount(), principal.id()));
    }

    @GetMapping("/subscriptions/{id}/payments")
    public List<PaymentResponse> payments(@PathVariable String id) {
        return subscriptionService.payments(id).stream().map(PaymentResponse::from).toList();
    }

    @GetMapping("/subscription-payments/{id}/receipt")
    public SubscriptionService.PaymentReceipt receipt(@PathVariable String id) {
        return subscriptionService.receipt(id);
    }
}
