package com.parking.infrastructure.config;

import com.parking.application.user.AdminBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, crea el primer administrador si todavia no existe ninguno (ADR-007). Los datos
 * vienen del entorno: PARKING_BOOTSTRAP_ADMIN_USERNAME, PARKING_BOOTSTRAP_ADMIN_PASSWORD y,
 * opcional, PARKING_BOOTSTRAP_ADMIN_FULL_NAME. La contraseña nunca se escribe en el log.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapService adminBootstrapService;
    private final String username;
    private final String fullName;
    private final String password;

    public AdminBootstrapRunner(AdminBootstrapService adminBootstrapService,
                                @Value("${parking.bootstrap.admin.username:}") String username,
                                @Value("${parking.bootstrap.admin.full-name:}") String fullName,
                                @Value("${parking.bootstrap.admin.password:}") String password) {
        this.adminBootstrapService = adminBootstrapService;
        this.username = username;
        this.fullName = fullName;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        switch (adminBootstrapService.bootstrapIfNeeded(username, fullName, password)) {
            case CREATED -> log.warn("Administrador inicial '{}' creado. Retire PARKING_BOOTSTRAP_ADMIN_PASSWORD "
                    + "del entorno y cambie la contraseña desde la aplicación.", username.trim());
            case ADMIN_ALREADY_EXISTS -> {
                if (!password.isBlank()) {
                    log.warn("Ya existe un administrador: se ignoran las variables PARKING_BOOTSTRAP_ADMIN_*. "
                            + "Retírelas del entorno.");
                }
            }
            case NOT_CONFIGURED -> log.warn("No existe ningún administrador y nadie podrá iniciar sesión. Defina "
                    + "PARKING_BOOTSTRAP_ADMIN_USERNAME y PARKING_BOOTSTRAP_ADMIN_PASSWORD (mínimo 8 caracteres) "
                    + "y reinicie el backend. Ver specs/decisions/ADR-007-arranque-produccion.md.");
        }
    }
}
