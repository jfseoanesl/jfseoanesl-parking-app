package com.parking.infrastructure.backup;

import com.parking.application.audit.AuditService;
import com.parking.infrastructure.backup.BackupFile.OperationCounter;
import com.parking.infrastructure.backup.BackupFile.Tables;
import com.parking.infrastructure.persistence.entity.RoleEntity;
import com.parking.infrastructure.persistence.entity.UserEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataAuditLogRepository;
import com.parking.infrastructure.persistence.jpa.SpringDataCashMovementRepository;
import com.parking.infrastructure.persistence.jpa.SpringDataCashSessionRepository;
import com.parking.infrastructure.persistence.jpa.SpringDataConfigurationRepository;
import com.parking.infrastructure.persistence.jpa.SpringDataParkingSessionRepository;
import com.parking.infrastructure.persistence.jpa.SpringDataPaymentMethodRepository;
import com.parking.infrastructure.persistence.jpa.SpringDataPaymentRepository;
import com.parking.infrastructure.persistence.jpa.SpringDataRoleRepository;
import com.parking.infrastructure.persistence.jpa.SpringDataTariffRepository;
import com.parking.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.parking.infrastructure.persistence.jpa.SpringDataVehicleTypeRepository;
import com.parking.domain.time.TimeProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RF-073 / ADR-009: exportación e importación completa de la base de datos. La importación
 * <b>reemplaza todo</b> en una sola transacción: si cualquier paso falla, no cambia nada.
 */
@Service
public class DatabaseBackupService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupService.class);

    private final SpringDataRoleRepository roles;
    private final SpringDataUserRepository users;
    private final SpringDataVehicleTypeRepository vehicleTypes;
    private final SpringDataPaymentMethodRepository paymentMethods;
    private final SpringDataConfigurationRepository configuration;
    private final SpringDataTariffRepository tariffs;
    private final SpringDataCashSessionRepository cashSessions;
    private final SpringDataParkingSessionRepository parkingSessions;
    private final SpringDataPaymentRepository payments;
    private final SpringDataCashMovementRepository cashMovements;
    private final SpringDataAuditLogRepository auditLogs;
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    @PersistenceContext
    private EntityManager entityManager;

    public DatabaseBackupService(SpringDataRoleRepository roles, SpringDataUserRepository users,
                                 SpringDataVehicleTypeRepository vehicleTypes,
                                 SpringDataPaymentMethodRepository paymentMethods,
                                 SpringDataConfigurationRepository configuration, SpringDataTariffRepository tariffs,
                                 SpringDataCashSessionRepository cashSessions,
                                 SpringDataParkingSessionRepository parkingSessions,
                                 SpringDataPaymentRepository payments, SpringDataCashMovementRepository cashMovements,
                                 SpringDataAuditLogRepository auditLogs, JdbcTemplate jdbcTemplate,
                                 AuditService auditService, TimeProvider timeProvider) {
        this.roles = roles;
        this.users = users;
        this.vehicleTypes = vehicleTypes;
        this.paymentMethods = paymentMethods;
        this.configuration = configuration;
        this.tariffs = tariffs;
        this.cashSessions = cashSessions;
        this.parkingSessions = parkingSessions;
        this.payments = payments;
        this.cashMovements = cashMovements;
        this.auditLogs = auditLogs;
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    /** Versión del esquema (Flyway) de esta instalación; un respaldo solo se importa en la misma. */
    public String schemaVersion() {
        // Se lee con la conexión de la transacción en curso: Flyway#info() pide otra conexión y, con
        // el pool de una sola conexión de SQLite, se bloquearía esperándola.
        List<String> versions = jdbcTemplate.query(
                "SELECT version, success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank DESC",
                (rs, i) -> rs.getBoolean("success") ? rs.getString("version") : null);
        return versions.stream().filter(Objects::nonNull).findFirst().orElse("0");
    }

    @Transactional
    public BackupFile export(String actingUserId, String actingUsername) {
        Tables tables = new Tables(
                roles.findAll(), users.findAll(), vehicleTypes.findAll(), paymentMethods.findAll(),
                configuration.findAll(), tariffs.findAll(), cashSessions.findAll(), parkingSessions.findAll(),
                payments.findAll(), cashMovements.findAll(), auditLogs.findAll(), readCounters());
        Map<String, Integer> counts = tables.counts();
        BackupFile backup = new BackupFile(BackupFile.FORMAT, BackupFile.FORMAT_VERSION, schemaVersion(),
                timeProvider.now(), actingUsername, databaseProductName(), counts, tables);

        // Se registra después de leer: el respaldo refleja el estado previo a su propia exportación.
        auditService.record(actingUserId, "DATABASE_EXPORTED", "Database", "backup", null, countsJson(counts));
        return backup;
    }

    @Transactional
    public Map<String, Integer> importReplacing(BackupFile backup, String actingUserId) {
        validate(backup);
        Tables t = backup.tables();

        // Borrado en orden inverso de llaves foráneas. Bulk delete no toca el contexto de
        // persistencia: se limpia para que ninguna entidad cargada antes (p. ej. el usuario de la
        // sesión) haga que merge() intente un UPDATE sobre una fila ya borrada.
        entityManager.flush();
        jdbcTemplate.update("DELETE FROM operation_counter");
        auditLogs.deleteAllInBatch();
        cashMovements.deleteAllInBatch();
        payments.deleteAllInBatch();
        parkingSessions.deleteAllInBatch();
        cashSessions.deleteAllInBatch();
        tariffs.deleteAllInBatch();
        configuration.deleteAllInBatch();
        paymentMethods.deleteAllInBatch();
        vehicleTypes.deleteAllInBatch();
        users.deleteAllInBatch();
        roles.deleteAllInBatch();
        entityManager.clear();

        // Inserción en orden de llaves foráneas; flush por tabla para que el orden de los INSERT sea
        // el declarado aunque Hibernate reordene acciones dentro de un mismo flush.
        insert(roles, t.roles());
        insert(users, t.users());
        insert(vehicleTypes, t.vehicleTypes());
        insert(paymentMethods, t.paymentMethods());
        insert(configuration, t.configuration());
        insert(tariffs, t.tariffs());
        insert(cashSessions, t.cashSessions());
        insert(parkingSessions, t.parkingSessions());
        insert(payments, t.payments());
        insert(cashMovements, t.cashMovements());
        insert(auditLogs, t.auditLogs());
        for (OperationCounter counter : t.operationCounters()) {
            jdbcTemplate.update("INSERT INTO operation_counter (name, counter_value) VALUES (?, ?)",
                    counter.name(), counter.counterValue());
        }
        entityManager.clear();

        Map<String, Integer> counts = t.counts();
        // audit_log.user_id es FK a app_user: solo se puede auditar si quien importa existe en los datos nuevos.
        boolean actorExists = t.users().stream().anyMatch(u -> Objects.equals(u.getId(), actingUserId));
        if (actorExists) {
            auditService.record(actingUserId, "DATABASE_IMPORTED", "Database", "backup", null,
                    "{\"exportedAt\":\"" + backup.exportedAt() + "\",\"counts\":" + countsJson(counts) + "}");
        } else {
            log.warn("Base de datos reemplazada desde un respaldo exportado el {} por '{}' ({}); el usuario {} que "
                            + "importó no existe en los datos restaurados, por eso no queda en audit_log. Conteos: {}",
                    backup.exportedAt(), backup.exportedBy(), backup.sourceDatabase(), actingUserId, counts);
        }
        return counts;
    }

    private void validate(BackupFile backup) {
        if (backup == null || !BackupFile.FORMAT.equals(backup.format())) {
            throw new IllegalArgumentException("El archivo no es un respaldo de esta aplicación");
        }
        if (backup.formatVersion() != BackupFile.FORMAT_VERSION) {
            throw new IllegalArgumentException("Versión de formato de respaldo no soportada: " + backup.formatVersion());
        }
        String installed = schemaVersion();
        if (!installed.equals(backup.schemaVersion())) {
            throw new IllegalArgumentException("El respaldo es de la versión " + backup.schemaVersion()
                    + " del esquema y esta instalación tiene la " + installed
                    + "; actualice ambas instalaciones a la misma versión de la aplicación");
        }
        if (backup.tables() == null || !backup.tables().complete()) {
            throw new IllegalArgumentException("El respaldo está incompleto: faltan tablas");
        }
        Set<String> adminRoleIds = backup.tables().roles().stream()
                .filter(r -> "ADMIN".equals(r.getCode())).map(RoleEntity::getId).collect(Collectors.toSet());
        boolean hasActiveAdmin = backup.tables().users().stream()
                .anyMatch(u -> u.isActive() && adminRoleIds.contains(u.getRoleId()));
        if (!hasActiveAdmin) {
            throw new IllegalArgumentException("El respaldo no tiene ningún administrador activo; "
                    + "importarlo dejaría la aplicación sin acceso");
        }
        boolean invalidUser = backup.tables().users().stream().map(UserEntity::getPasswordHash)
                .anyMatch(h -> h == null || !h.startsWith("$2"));
        if (invalidUser) {
            throw new IllegalArgumentException("El respaldo contiene usuarios sin contraseña cifrada válida");
        }
    }

    private static <E> void insert(JpaRepository<E, String> repository, List<E> rows) {
        repository.saveAllAndFlush(rows);
    }

    private List<OperationCounter> readCounters() {
        return jdbcTemplate.query("SELECT name, counter_value FROM operation_counter ORDER BY name",
                (rs, i) -> new OperationCounter(rs.getString("name"), rs.getLong("counter_value")));
    }

    private String databaseProductName() {
        return jdbcTemplate.execute((ConnectionCallback<String>) c -> c.getMetaData().getDatabaseProductName());
    }

    private static String countsJson(Map<String, Integer> counts) {
        return counts.entrySet().stream().map(e -> "\"" + e.getKey() + "\":" + e.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }
}
