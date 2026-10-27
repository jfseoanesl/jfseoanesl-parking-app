package com.parking.infrastructure.backup;

import com.parking.infrastructure.persistence.entity.AuditLogEntity;
import com.parking.infrastructure.persistence.entity.CashMovementEntity;
import com.parking.infrastructure.persistence.entity.CashSessionEntity;
import com.parking.infrastructure.persistence.entity.ConfigurationEntity;
import com.parking.infrastructure.persistence.entity.ParkingSessionEntity;
import com.parking.infrastructure.persistence.entity.PaymentEntity;
import com.parking.infrastructure.persistence.entity.PaymentMethodEntity;
import com.parking.infrastructure.persistence.entity.RoleEntity;
import com.parking.infrastructure.persistence.entity.TariffEntity;
import com.parking.infrastructure.persistence.entity.UserEntity;
import com.parking.infrastructure.persistence.entity.VehicleTypeEntity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-073 / ADR-009: respaldo completo y portable (independiente del motor: SQLite o MySQL). Las filas
 * se serializan desde las entidades JPA, así que cada motor convierte sus propios tipos y el JSON
 * queda neutro. {@code schemaVersion} es la versión Flyway: solo se importa en la misma versión.
 */
public record BackupFile(
        String format,
        int formatVersion,
        String schemaVersion,
        Instant exportedAt,
        String exportedBy,
        String sourceDatabase,
        Map<String, Integer> counts,
        Tables tables
) {

    public static final String FORMAT = "parking-backup";
    public static final int FORMAT_VERSION = 1;

    /** En orden de inserción según las llaves foráneas (el borrado va en orden inverso). */
    public record Tables(
            List<RoleEntity> roles,
            List<UserEntity> users,
            List<VehicleTypeEntity> vehicleTypes,
            List<PaymentMethodEntity> paymentMethods,
            List<ConfigurationEntity> configuration,
            List<TariffEntity> tariffs,
            /** RF-074 (V7): mensualidades, sus placas autorizadas y sus pagos. */
            List<com.parking.infrastructure.persistence.entity.SubscriptionEntity> subscriptions,
            List<com.parking.infrastructure.persistence.entity.SubscriptionVehicleEntity> subscriptionVehicles,
            List<CashSessionEntity> cashSessions,
            List<com.parking.infrastructure.persistence.entity.SubscriptionPaymentEntity> subscriptionPayments,
            /** UC-014 (V9): catálogo de conceptos y otros ingresos/gastos de caja. */
            List<com.parking.infrastructure.persistence.entity.CashConceptEntity> cashConcepts,
            List<com.parking.infrastructure.persistence.entity.CashTransactionEntity> cashTransactions,
            List<ParkingSessionEntity> parkingSessions,
            List<PaymentEntity> payments,
            List<CashMovementEntity> cashMovements,
            List<AuditLogEntity> auditLogs,
            List<OperationCounter> operationCounters
    ) {

        public Map<String, Integer> counts() {
            Map<String, Integer> counts = new LinkedHashMap<>();
            counts.put("roles", size(roles));
            counts.put("users", size(users));
            counts.put("vehicleTypes", size(vehicleTypes));
            counts.put("paymentMethods", size(paymentMethods));
            counts.put("configuration", size(configuration));
            counts.put("tariffs", size(tariffs));
            counts.put("subscriptions", size(subscriptions));
            counts.put("subscriptionVehicles", size(subscriptionVehicles));
            counts.put("cashSessions", size(cashSessions));
            counts.put("subscriptionPayments", size(subscriptionPayments));
            counts.put("cashConcepts", size(cashConcepts));
            counts.put("cashTransactions", size(cashTransactions));
            counts.put("parkingSessions", size(parkingSessions));
            counts.put("payments", size(payments));
            counts.put("cashMovements", size(cashMovements));
            counts.put("auditLogs", size(auditLogs));
            counts.put("operationCounters", size(operationCounters));
            return counts;
        }

        boolean complete() {
            return roles != null && users != null && vehicleTypes != null && paymentMethods != null
                    && configuration != null && tariffs != null && subscriptions != null
                    && subscriptionVehicles != null && cashSessions != null && subscriptionPayments != null
                    && cashConcepts != null && cashTransactions != null && parkingSessions != null
                    && payments != null && cashMovements != null && auditLogs != null && operationCounters != null;
        }

        private static int size(List<?> list) {
            return list == null ? 0 : list.size();
        }
    }

    /** Fila de {@code operation_counter} (consecutivos OP y ING, V5/V6). */
    public record OperationCounter(String name, long counterValue) {
    }
}
