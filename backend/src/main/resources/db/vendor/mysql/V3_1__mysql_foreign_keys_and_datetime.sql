-- Fase 12 - Preparacion MySQL. Migracion EXCLUSIVA de MySQL (Flyway: classpath:db/vendor/{vendor};
-- SQLite nunca la ve). Corrige dos diferencias de motor encontradas al ejecutar V1 sobre MySQL 8.4,
-- sin tocar V1/V3 (ya aplicadas en las bases SQLite; editarlas romperia su checksum de Flyway):
--
-- 1. Llaves foraneas: V1 las declara "en linea" (col CHAR(36) REFERENCES t(id)). SQLite las aplica
--    (foreign_keys=on), pero MySQL analiza y DESCARTA en silencio esa sintaxis: el esquema quedaba
--    sin ninguna FK. Se agregan como restricciones de tabla, que MySQL si crea.
-- 2. Fechas: TIMESTAMP en MySQL solo llega a 2038-01-19 y, sin precision, trunca los milisegundos.
--    Se pasa a DATETIME(6). Los valores son instantes UTC (el JDBC usa connectionTimeZone=UTC), igual
--    que antes; no cambia ningun dato ni la logica de dominio.
--
-- Numeracion: 3.1 queda entre V3 (comun) y la siguiente comun (V4+), que debe seguir funcionando
-- igual en ambos motores. Ver specs/decisions/ADR-006-migracion-mysql.md.

ALTER TABLE app_user
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL,
    ADD CONSTRAINT fk_app_user_role FOREIGN KEY (role_id) REFERENCES role (id);

ALTER TABLE tariff
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL,
    ADD CONSTRAINT fk_tariff_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type (id),
    ADD CONSTRAINT fk_tariff_created_by FOREIGN KEY (created_by) REFERENCES app_user (id),
    ADD CONSTRAINT fk_tariff_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id);

ALTER TABLE cash_session
    MODIFY opened_at DATETIME(6) NOT NULL,
    MODIFY closed_at DATETIME(6) NULL,
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL,
    ADD CONSTRAINT fk_cash_session_employee FOREIGN KEY (employee_id) REFERENCES app_user (id);

ALTER TABLE parking_session
    MODIFY entry_datetime DATETIME(6) NOT NULL,
    MODIFY exit_datetime DATETIME(6) NULL,
    MODIFY cancelled_at DATETIME(6) NULL,
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL,
    ADD CONSTRAINT fk_parking_session_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type (id),
    ADD CONSTRAINT fk_parking_session_entry_employee FOREIGN KEY (entry_employee_id) REFERENCES app_user (id),
    ADD CONSTRAINT fk_parking_session_exit_employee FOREIGN KEY (exit_employee_id) REFERENCES app_user (id),
    ADD CONSTRAINT fk_parking_session_tariff FOREIGN KEY (applied_tariff_id) REFERENCES tariff (id),
    ADD CONSTRAINT fk_parking_session_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES app_user (id);

ALTER TABLE payment
    MODIFY paid_at DATETIME(6) NOT NULL,
    MODIFY created_at DATETIME(6) NOT NULL,
    ADD CONSTRAINT fk_payment_parking_session FOREIGN KEY (parking_session_id) REFERENCES parking_session (id),
    ADD CONSTRAINT fk_payment_cash_session FOREIGN KEY (cash_session_id) REFERENCES cash_session (id),
    ADD CONSTRAINT fk_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_method (id),
    ADD CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES app_user (id);

ALTER TABLE cash_movement
    MODIFY created_at DATETIME(6) NOT NULL,
    ADD CONSTRAINT fk_cash_movement_cash_session FOREIGN KEY (cash_session_id) REFERENCES cash_session (id),
    ADD CONSTRAINT fk_cash_movement_payment FOREIGN KEY (payment_id) REFERENCES payment (id);

ALTER TABLE configuration
    MODIFY updated_at DATETIME(6) NOT NULL,
    ADD CONSTRAINT fk_configuration_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id);

ALTER TABLE audit_log
    MODIFY occurred_at DATETIME(6) NOT NULL,
    ADD CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES app_user (id);
