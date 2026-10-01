package com.parking.application.auth;

import com.parking.domain.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteccion contra fuerza bruta en el login (revision OWASP A07, 2026-09-29): tras
 * {@link #MAX_FAILURES} fallos para un mismo usuario dentro de {@link #WINDOW}, ese usuario queda
 * bloqueado durante {@link #LOCK_DURATION}. El contador se reinicia al iniciar sesion con exito.
 * Tambien registra en el log cada fallo y cada bloqueo (OWASP A09), nunca la contrasena.
 *
 * <p>Se lleva por nombre de usuario (no por IP): detras del proxy nginx todas las peticiones llegan
 * con la misma IP. En memoria: se reinicia con el backend, suficiente para una sola instancia.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    /** Tope de usuarios rastreados, para que nombres aleatorios no agoten la memoria. */
    private static final int MAX_TRACKED = 10_000;

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private record Attempts(int failures, Instant firstFailure, Instant lockedUntil) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final TimeProvider timeProvider;

    public LoginAttemptService(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    /** @throws LoginBlockedException si el usuario esta bloqueado en este momento. */
    public void checkNotBlocked(String username) {
        Attempts current = attempts.get(key(username));
        Instant now = timeProvider.now();
        if (current != null && current.lockedUntil() != null && now.isBefore(current.lockedUntil())) {
            throw new LoginBlockedException(Duration.between(now, current.lockedUntil()));
        }
    }

    public void recordFailure(String username, String clientAddress) {
        Instant now = timeProvider.now();
        if (attempts.size() > MAX_TRACKED) {
            attempts.values().removeIf(a -> isExpired(a, now));
        }
        Attempts updated = attempts.compute(key(username), (k, previous) -> {
            boolean restart = previous == null || isExpired(previous, now);
            int failures = restart ? 1 : previous.failures() + 1;
            Instant first = restart ? now : previous.firstFailure();
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;
            return new Attempts(failures, first, lockedUntil);
        });
        if (updated.lockedUntil() != null) {
            log.warn("Usuario '{}' bloqueado {} min tras {} intentos fallidos de inicio de sesión (último desde {})",
                    sanitize(username), LOCK_DURATION.toMinutes(), updated.failures(), clientAddress);
        } else {
            log.warn("Inicio de sesión fallido para el usuario '{}' desde {} (intento {} de {})",
                    sanitize(username), clientAddress, updated.failures(), MAX_FAILURES);
        }
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private boolean isExpired(Attempts a, Instant now) {
        boolean windowOver = now.isAfter(a.firstFailure().plus(WINDOW));
        boolean lockOver = a.lockedUntil() == null || !now.isBefore(a.lockedUntil());
        return windowOver && lockOver;
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    /** Evita inyeccion en el log (saltos de linea) con nombres de usuario manipulados. */
    private static String sanitize(String username) {
        if (username == null) return "";
        String clean = username.replaceAll("[\\r\\n\\t]", "_");
        return clean.length() > 60 ? clean.substring(0, 60) + "…" : clean;
    }
}
