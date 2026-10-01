package com.parking.domain.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Servicio de tiempo centralizado (BR-026, RNF-025). Toda fecha/hora del sistema pasa por aqui
 * en vez de llamar a Instant.now()/LocalDateTime.now() directamente en servicios individuales -
 * facilita pruebas (se puede sustituir por una implementacion fija) y evita logica de fecha
 * dispersa por el sistema.
 */
public interface TimeProvider {

    /** Instante actual en UTC, tal como se persiste (07-data-model.md). */
    Instant now();

    /** Zona horaria configurada del parqueadero (semilla: America/Bogota). */
    ZoneId zone();

    /** Instante actual convertido a la zona horaria configurada, para calculos de franja horaria. */
    default ZonedDateTime nowInZone() {
        return now().atZone(zone());
    }
}
