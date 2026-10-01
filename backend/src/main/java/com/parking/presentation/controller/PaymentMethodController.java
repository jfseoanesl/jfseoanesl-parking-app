package com.parking.presentation.controller;

import com.parking.domain.exception.BusinessRuleViolationException;
import com.parking.domain.model.PaymentMethod;
import com.parking.domain.repository.PaymentMethodRepository;
import com.parking.presentation.dto.CreatePaymentMethodRequest;
import com.parking.presentation.dto.PaymentMethodResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodController(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<PaymentMethodResponse> list() {
        return paymentMethodRepository.findAllActive().stream().map(PaymentMethodResponse::from).toList();
    }

    /** RF-014: catálogo de medios de pago parametrizable (BR-018). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponse create(@Valid @RequestBody CreatePaymentMethodRequest request) {
        String code = request.code().trim().toUpperCase();
        if (paymentMethodRepository.existsByCode(code)) {
            throw new BusinessRuleViolationException("PAYMENT_METHOD_CODE_TAKEN",
                    "Ya existe un medio de pago con ese código");
        }
        PaymentMethod saved = paymentMethodRepository.save(
                new PaymentMethod(UUID.randomUUID().toString(), code, request.name(), request.category(), true));
        return PaymentMethodResponse.from(saved);
    }
}
