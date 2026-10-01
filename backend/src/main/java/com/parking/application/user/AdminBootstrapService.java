package com.parking.application.user;

import com.parking.application.audit.AuditService;
import com.parking.domain.model.Role;
import com.parking.domain.model.User;
import com.parking.domain.repository.RoleRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Crea el primer administrador de una instalacion nueva (ADR-007) a partir de datos provistos por
 * el entorno, nunca con credenciales por defecto. Solo actua si no existe ningun administrador: una
 * vez creado, reiniciar con las mismas variables no hace nada. La contraseña se guarda con hash
 * (BR-004) y la creacion queda en auditoria (BR-025), con el propio administrador como actor.
 */
@Service
public class AdminBootstrapService {

    /** Misma politica minima que el alta de usuarios por API (CreateUserRequest). */
    static final int MIN_PASSWORD_LENGTH = 8;

    public enum Outcome {
        /** Se creo el administrador. */
        CREATED,
        /** Ya habia un administrador; no se hizo nada. */
        ADMIN_ALREADY_EXISTS,
        /** No hay administrador y no se indicaron los datos para crearlo. */
        NOT_CONFIGURED
    }

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public AdminBootstrapService(UserRepository userRepository, RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder, AuditService auditService,
                                 TimeProvider timeProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    /**
     * @throws IllegalStateException si los datos son invalidos (contraseña corta, usuario ya tomado,
     *                               o falta el rol ADMIN): un error de configuracion debe detener el
     *                               arranque en vez de pasar desapercibido.
     */
    @Transactional
    public Outcome bootstrapIfNeeded(String username, String fullName, String rawPassword) {
        Role adminRole = roleRepository.findByCode("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el rol ADMIN: la migracion V4 (datos de referencia) no se aplico"));

        boolean adminExists = userRepository.findAll().stream().anyMatch(u -> u.roleId().equals(adminRole.id()));
        if (adminExists) {
            return Outcome.ADMIN_ALREADY_EXISTS;
        }
        if (isBlank(username) || isBlank(rawPassword)) {
            return Outcome.NOT_CONFIGURED;
        }
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("La contraseña del administrador inicial debe tener al menos "
                    + MIN_PASSWORD_LENGTH + " caracteres");
        }
        if (userRepository.existsByUsername(username.trim())) {
            throw new IllegalStateException("Ya existe un usuario '" + username.trim()
                    + "' que no es administrador; elija otro nombre para el administrador inicial");
        }

        Instant now = timeProvider.now();
        User admin = new User(UUID.randomUUID().toString(), username.trim(), passwordEncoder.encode(rawPassword),
                isBlank(fullName) ? "Administrador" : fullName.trim(), adminRole.id(), true, now, now);
        User saved = userRepository.save(admin);

        // El actor es el propio administrador: no existe ningun otro usuario (y audit_log exige uno real).
        auditService.record(saved.id(), "USER_CREATED", "User", saved.id(), null,
                "{\"username\":\"" + saved.username().replace("\"", "'") + "\",\"role\":\"ADMIN\",\"bootstrap\":true}");
        return Outcome.CREATED;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
