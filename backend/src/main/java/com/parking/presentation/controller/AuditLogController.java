package com.parking.presentation.controller;

import com.parking.domain.model.AuditLog;
import com.parking.domain.model.User;
import com.parking.domain.repository.AuditLogRepository;
import com.parking.domain.repository.UserRepository;
import com.parking.presentation.dto.AuditLogResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** UC-012 (RF-067). */
@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final com.parking.domain.time.TimeProvider timeProvider;

    public AuditLogController(AuditLogRepository auditLogRepository, UserRepository userRepository,
                              com.parking.domain.time.TimeProvider timeProvider) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    /** RF-067: filtros por entidad, id de entidad, usuario y rango de fechas (zona del parqueadero). */
    public List<AuditLogResponse> list(@RequestParam(required = false) String entity,
                                        @RequestParam(required = false) String entityId,
                                        @RequestParam(required = false) String userId,
                                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(
                                                iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                        java.time.LocalDate from,
                                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(
                                                iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                        java.time.LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        java.time.ZoneId zone = timeProvider.zone();
        java.time.Instant start = from == null ? null : from.atStartOfDay(zone).toInstant();
        java.time.Instant end = to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);
        return auditLogRepository.search(blankToNull(entity), blankToNull(entityId), blankToNull(userId), start, end)
                .stream().map(this::toResponse).toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private AuditLogResponse toResponse(AuditLog log) {
        String userFullName = userRepository.findById(log.userId()).map(User::fullName).orElse(log.userId());
        return new AuditLogResponse(log.id(), log.userId(), userFullName, log.action(), log.entityName(),
                log.entityId(), log.oldValue(), log.newValue(), log.occurredAt());
    }
}
