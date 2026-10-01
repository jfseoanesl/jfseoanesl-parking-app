package com.parking.application.user;

import com.parking.application.audit.AuditService;
import com.parking.domain.exception.BusinessRuleViolationException;
import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.Role;
import com.parking.domain.model.User;
import com.parking.domain.repository.RoleRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** UC-009 (RF-005 a RF-009). BR-003 (RBAC), BR-004 (hash), BR-025 (auditoría de usuarios). */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder, AuditService auditService, TimeProvider timeProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    public List<User> listAll() {
        return userRepository.findAll();
    }

    @Transactional
    public User create(String username, String fullName, String rawPassword, String roleCode, String actorId) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessRuleViolationException("USERNAME_TAKEN", "Ya existe un usuario con ese nombre");
        }
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Rol inválido: " + roleCode));

        Instant now = timeProvider.now();
        User user = new User(UUID.randomUUID().toString(), username, passwordEncoder.encode(rawPassword),
                fullName, role.id(), true, now, now);
        User saved = userRepository.save(user);

        auditService.record(actorId, "USER_CREATED", "User", saved.id(), null,
                "{\"username\":\"" + username + "\",\"role\":\"" + roleCode + "\"}");
        return saved;
    }

    @Transactional
    public User setActive(String userId, boolean active, String actorId) {
        User user = getOrThrow(userId);
        User updated = new User(user.id(), user.username(), user.passwordHash(), user.fullName(), user.roleId(),
                active, user.createdAt(), timeProvider.now());
        User saved = userRepository.save(updated);

        auditService.record(actorId, active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "User", userId,
                "{\"active\":" + user.active() + "}", "{\"active\":" + active + "}");
        return saved;
    }

    @Transactional
    public void changePassword(String targetUserId, String newPassword, String actorId, boolean actorIsAdmin) {
        User user = getOrThrow(targetUserId);
        if (!actorIsAdmin && !actorId.equals(targetUserId)) {
            throw new BusinessRuleViolationException("FORBIDDEN_PASSWORD_CHANGE",
                    "No puede cambiar la contraseña de otro usuario");
        }
        User updated = new User(user.id(), user.username(), passwordEncoder.encode(newPassword), user.fullName(),
                user.roleId(), user.active(), user.createdAt(), timeProvider.now());
        userRepository.save(updated);

        // BR-025: se audita el cambio, nunca el valor de la contraseña (RNF-013).
        auditService.record(actorId, "USER_PASSWORD_CHANGED", "User", targetUserId, null, null);
    }

    /**
     * Cambio de la propia contrasena: exige la actual (revision OWASP A07). Sin esto, quien tomara una
     * sesion abierta ajena podia fijar una contrasena nueva y quedarse con la cuenta.
     */
    @Transactional
    public void changeOwnPassword(String userId, String currentPassword, String newPassword) {
        User user = getOrThrow(userId);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new BusinessRuleViolationException("INVALID_CURRENT_PASSWORD", "La contraseña actual no es correcta");
        }
        changePassword(userId, newPassword, userId, false);
    }

    private User getOrThrow(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}
