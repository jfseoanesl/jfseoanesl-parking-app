package com.parking.infrastructure.time;

import com.parking.domain.time.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Implementacion por defecto de TimeProvider (BR-026). La zona horaria parte de
 * "parking.timezone" (application.yml, semilla America/Bogota); en la Fase 7
 * (Configuracion) pasara a leerse de la tabla Configuration en lugar de una propiedad fija.
 */
@Component
public class SystemTimeProvider implements TimeProvider {

    private final ZoneId zoneId;

    public SystemTimeProvider(@Value("${parking.timezone:America/Bogota}") String timezone) {
        this.zoneId = ZoneId.of(timezone);
    }

    @Override
    public Instant now() {
        return Instant.now();
    }

    @Override
    public ZoneId zone() {
        return zoneId;
    }
}
