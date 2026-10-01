package com.parking.presentation.controller;

import com.parking.application.payment.ReceiptData;
import com.parking.application.payment.ReceiptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF-050. Ver 08-api-contract.md §Pagos. */
@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
public class PaymentController {

    private final ReceiptService receiptService;

    public PaymentController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/{id}/receipt")
    public ReceiptData receipt(@PathVariable String id) {
        return receiptService.getByPaymentId(id);
    }
}
