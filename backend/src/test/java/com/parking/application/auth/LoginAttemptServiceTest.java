package com.parking.application.auth;

import com.parking.domain.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** OWASP A07: ventanas y bloqueo del limite de intentos, con reloj controlado. */
class LoginAttemptServiceTest {

    private Instant now = Instant.parse("2026-09-29T12:00:00Z");
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(new TimeProvider() {
            @Override
            public Instant now() {
                return now;
            }

            @Override
            public ZoneId zone() {
                return ZoneId.of("America/Bogota");
            }
        });
    }

    private void fail(String user, int times) {
        for (int i = 0; i < times; i++) service.recordFailure(user, "127.0.0.1");
    }

    @Test
    void cuatroFallosNoBloquean() {
        fail("ana", LoginAttemptService.MAX_FAILURES - 1);
        assertThatCode(() -> service.checkNotBlocked("ana")).doesNotThrowAnyException();
    }

    @Test
    void elQuintoFalloBloqueaYAlVencerSeDesbloquea() {
        fail("ana", LoginAttemptService.MAX_FAILURES);
        assertThatThrownBy(() -> service.checkNotBlocked("ana")).isInstanceOf(LoginBlockedException.class);

        now = now.plus(LoginAttemptService.LOCK_DURATION).plus(Duration.ofSeconds(1));
        assertThatCode(() -> service.checkNotBlocked("ana")).doesNotThrowAnyException();
    }

    @Test
    void elBloqueoNoDistingueMayusculasNiEspacios() {
        fail(" ANA ", LoginAttemptService.MAX_FAILURES);
        assertThatThrownBy(() -> service.checkNotBlocked("ana")).isInstanceOf(LoginBlockedException.class);
    }

    @Test
    void unLoginExitosoReiniciaElContador() {
        fail("ana", LoginAttemptService.MAX_FAILURES - 1);
        service.recordSuccess("ana");
        fail("ana", LoginAttemptService.MAX_FAILURES - 1);
        assertThatCode(() -> service.checkNotBlocked("ana")).doesNotThrowAnyException();
    }

    @Test
    void fallosFueraDeLaVentanaNoSeAcumulan() {
        fail("ana", LoginAttemptService.MAX_FAILURES - 1);
        now = now.plus(LoginAttemptService.WINDOW).plus(Duration.ofMinutes(1));
        fail("ana", 1);
        assertThatCode(() -> service.checkNotBlocked("ana")).doesNotThrowAnyException();
    }

    @Test
    void bloquearAUnUsuarioNoAfectaAOtro() {
        fail("ana", LoginAttemptService.MAX_FAILURES);
        assertThatCode(() -> service.checkNotBlocked("beto")).doesNotThrowAnyException();
    }
}
