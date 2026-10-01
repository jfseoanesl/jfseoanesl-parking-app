package com.parking.application.user;

import com.parking.application.audit.AuditService;
import com.parking.application.user.AdminBootstrapService.Outcome;
import com.parking.domain.model.Role;
import com.parking.domain.model.User;
import com.parking.domain.repository.RoleRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.domain.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ADR-007: creacion del primer administrador desde el entorno. */
class AdminBootstrapServiceTest {

    private static final Role ADMIN = new Role("role-admin", "ADMIN", "Administrador");
    private static final Role EMPLOYEE = new Role("role-emp", "EMPLOYEE", "Empleado");

    private UserRepository userRepository;
    private AuditService auditService;
    private AdminBootstrapService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        auditService = mock(AuditService.class);
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.of(ADMIN));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        TimeProvider timeProvider = new TimeProvider() {
            @Override
            public Instant now() {
                return Instant.parse("2026-09-29T12:00:00Z");
            }

            @Override
            public ZoneId zone() {
                return ZoneId.of("America/Bogota");
            }
        };
        service = new AdminBootstrapService(userRepository, roleRepository, new BCryptPasswordEncoder(),
                auditService, timeProvider);
    }

    private static User user(String username, Role role) {
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        return new User("u-" + username, username, "hash", username, role.id(), true, t, t);
    }

    @Test
    void creaElAdministradorConHashYAuditoriaCuandoNoExisteNinguno() {
        when(userRepository.findAll()).thenReturn(List.of(user("cajero", EMPLOYEE)));

        Outcome outcome = service.bootstrapIfNeeded(" jefe ", "Jefa de Operación", "clave-segura-1");

        assertEquals(Outcome.CREATED, outcome);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("jefe", saved.getValue().username());
        assertEquals(ADMIN.id(), saved.getValue().roleId());
        assertTrue(saved.getValue().active());
        // BR-004: nunca en texto plano.
        assertNotEquals("clave-segura-1", saved.getValue().passwordHash());
        assertTrue(new BCryptPasswordEncoder().matches("clave-segura-1", saved.getValue().passwordHash()));
        // BR-025: auditado, con el propio administrador como actor y sin la contraseña.
        String id = saved.getValue().id();
        verify(auditService).record(eq(id), eq("USER_CREATED"), eq("User"), eq(id), isNull(), contains("\"bootstrap\":true"));
    }

    @Test
    void noHaceNadaSiYaExisteUnAdministrador() {
        when(userRepository.findAll()).thenReturn(List.of(user("admin", ADMIN)));

        assertEquals(Outcome.ADMIN_ALREADY_EXISTS, service.bootstrapIfNeeded("otro", null, "clave-segura-1"));
        verify(userRepository, never()).save(any());
        verify(auditService, never()).record(anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void sinDatosDeEntornoInformaQueNoEstaConfigurado() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertEquals(Outcome.NOT_CONFIGURED, service.bootstrapIfNeeded("", "", ""));
        assertEquals(Outcome.NOT_CONFIGURED, service.bootstrapIfNeeded("jefe", null, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rechazaUnaContrasenaCorta() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.bootstrapIfNeeded("jefe", null, "corta"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rechazaUnNombreDeUsuarioYaTomado() {
        when(userRepository.findAll()).thenReturn(List.of(user("jefe", EMPLOYEE)));
        when(userRepository.existsByUsername("jefe")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.bootstrapIfNeeded("jefe", null, "clave-segura-1"));
        verify(userRepository, never()).save(any());
    }
}
