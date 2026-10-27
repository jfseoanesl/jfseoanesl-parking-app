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
        BigDecimal expectedCash,
        /** BR-033: otros ingresos activos (ya incluidos en los totales por medio de pago). */
        BigDecimal otherIncomeCollected,
        /** BR-033: gastos activos de la caja; solo los de efectivo (expensesCash) restan del efectivo esperado. */
        BigDecimal expensesTotal,
        BigDecimal expensesCash
) {
}
