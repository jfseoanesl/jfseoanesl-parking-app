package com.parking.application.payment;

import java.math.BigDecimal;
import java.time.Instant;

/** RF-050: datos mínimos del comprobante (prompt.md §11), ya resueltos (sin IDs sueltos). */
public record ReceiptData(
        String parkingName,
        String nit,
        String address,
        String phone,
        String plate,
        String vehicleTypeName,
        Instant entryDateTime,
        Instant exitDateTime,
        long elapsedMinutes,
        String tariffName,
        BigDecimal chargedAmount,
        String paymentMethodName,
        String operationNumber,
        String employeeName
) {
}
