package com.parking.application.parking;

import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.Payment;
import com.parking.domain.model.PaymentMethod;
import com.parking.domain.tariffengine.TariffCalculationResult;

/** Resultado de UC-004 (checkout) para que el controller lo proyecte en la respuesta HTTP. */
public record CheckoutResult(
        ParkingSession session,
        TariffCalculationResult tariffResult,
        Payment payment,
        PaymentMethod paymentMethod
) {
}
