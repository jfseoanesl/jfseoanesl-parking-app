package com.parking.domain.model;

/** BR-028: estado de una mensualidad en una fecha dada. */
public enum SubscriptionStatus {
    /** Hoy está dentro del periodo pagado. */
    AL_DIA,
    /** Pasó el corte, pero aún dentro de los días de gracia: la salida sigue sin cobro. */
    EN_GRACIA,
    /** Pasaron los días de gracia: la salida se cobra como cliente normal. */
    VENCIDA,
    /** Nunca se ha pagado. */
    SIN_PAGO,
    /** Desactivada por el administrador. */
    INACTIVA;

    /** BR-030: la salida no se cobra. */
    public boolean coversExit() {
        return this == AL_DIA || this == EN_GRACIA;
    }
}
