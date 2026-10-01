package com.parking.application.auth;

import java.time.Duration;

/** Demasiados intentos fallidos de inicio de sesion para un usuario (OWASP A07). Se responde 429. */
public class LoginBlockedException extends RuntimeException {

    private final Duration retryAfter;

    public LoginBlockedException(Duration retryAfter) {
        super("Demasiados intentos fallidos. Intente de nuevo en "
                + Math.max(1, (retryAfter.toSeconds() + 59) / 60) + " minuto(s).");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
