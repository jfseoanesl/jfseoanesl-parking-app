package com.parking.application.cash;

import java.math.BigDecimal;

/** RF-055: totales de una caja (en vivo antes del cierre, o congelados tras cerrarla). */
public record CashSessionSummary(
        BigDecimal initialBalance,
        BigDecimal cashCollected,
        BigDecimal electronicCollected,
        BigDecimal otherCollected,
        BigDecimal totalCollected,
        int vehiclesServed,
        BigDecimal expectedCash
) {
}
