package com.parking.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * RF-074: mensualidad de un cliente con valor y día de corte pactados. {@code paidUntil} es el último
 * día cubierto por los pagos (null = nunca pagó). Las placas autorizadas viven en {@code vehicles}.
 */
public record Subscription(
        String id,
        String customerName,
        String documentId,
        String phone,
        BigDecimal monthlyFee,
        int cutOffDay,
        LocalDate paidUntil,
        boolean active,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        List<SubscriptionVehicle> vehicles
) {

    public Subscription withVehicles(List<SubscriptionVehicle> newVehicles) {
        return new Subscription(id, customerName, documentId, phone, monthlyFee, cutOffDay, paidUntil, active,
                notes, createdAt, updatedAt, createdBy, newVehicles);
    }
}
