package com.parking.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fase 12: la integridad referencial la garantiza la base de datos, no solo la aplicacion. Corre
 * igual sobre SQLite (perfil test) y sobre MySQL (procedimiento de verificacion de
 * specs/decisions/ADR-006). Regresion: MySQL descarta en silencio las FK declaradas "en linea" en
 * V1 y el esquema quedaba sin ninguna; lo corrige db/vendor/mysql/V3_1.
 */
@SpringBootTest
@ActiveProfiles("test")
class SchemaIntegrityTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void laBaseRechazaUnaReferenciaAUnaFilaInexistente() {
        DataAccessException ex = assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO cash_movement (id, cash_session_id, type, amount, created_at) "
                        + "VALUES ('fk-probe', 'caja-inexistente', 'ADJUSTMENT', 1, CURRENT_TIMESTAMP)"));

        String message = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
        assertTrue(message.contains("foreign key"), "Se esperaba un error de llave foranea, fue: " + message);
    }
}
