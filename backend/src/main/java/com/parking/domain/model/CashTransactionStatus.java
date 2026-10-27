package com.parking.domain.model;

/** BR-035: un movimiento anulado deja de sumar en la caja, el Dashboard y los reportes. */
public enum CashTransactionStatus {
    ACTIVE,
    VOIDED
}
