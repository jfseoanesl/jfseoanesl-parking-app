package com.parking.domain.model;

/** Placa autorizada de una mensualidad (BR-029). La placa va normalizada (BR-002) y es única. */
public record SubscriptionVehicle(String id, String subscriptionId, String plate, String vehicleTypeId) {
}
