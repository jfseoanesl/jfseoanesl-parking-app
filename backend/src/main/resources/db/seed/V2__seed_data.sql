-- Datos semilla para desarrollo/pruebas (prompt.md §33). Ubicado en db/seed (no db/migration) para
-- que solo se aplique en los perfiles development/test (ver application-*.yml, spring.flyway.locations).
-- NUNCA usar estas credenciales en produccion.

INSERT INTO role (id, code, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Administrador'),
    ('00000000-0000-0000-0000-000000000002', 'EMPLOYEE', 'Empleado');

INSERT INTO vehicle_type (id, code, name, active) VALUES
    ('00000000-0000-0000-0000-000000000101', 'CAR', 'Automóvil', TRUE),
    ('00000000-0000-0000-0000-000000000102', 'MOTORCYCLE', 'Motocicleta', TRUE);

INSERT INTO payment_method (id, code, name, active) VALUES
    ('00000000-0000-0000-0000-000000000201', 'CASH', 'Efectivo', TRUE),
    ('00000000-0000-0000-0000-000000000202', 'TRANSFER', 'Transferencia', TRUE),
    ('00000000-0000-0000-0000-000000000203', 'OTHER', 'Otros', TRUE);

INSERT INTO configuration (id, parking_name, nit, address, phone, max_capacity, timezone, currency, updated_at)
VALUES ('00000000-0000-0000-0000-000000000301', 'Parqueadero de Prueba', '900123456-7',
        'Calle Falsa 123', '3000000000', 40, 'America/Bogota', 'COP', '2026-01-01 00:00:00.000');

-- Usuario administrador de prueba: usuario "admin", contraseña "admin123" (BCrypt).
INSERT INTO app_user (id, username, password_hash, full_name, role_id, active, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000401', 'admin',
        '$2a$10$1af5gNP9FiK1q4oEg471KOt/zclXay/ai02Qjfi5aBS2CWZyiD1E.',
        'Administrador de Prueba', '00000000-0000-0000-0000-000000000001', TRUE,
        '2026-01-01 00:00:00.000', '2026-01-01 00:00:00.000');

-- Usuario empleado de prueba: usuario "empleado", contraseña "empleado123" (BCrypt).
INSERT INTO app_user (id, username, password_hash, full_name, role_id, active, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000402', 'empleado',
        '$2a$10$tzUb8JYoKb0a0LPMvLk6i.8hY29yRX95.ros77ZXEm1yywXnHencq',
        'Empleado de Prueba', '00000000-0000-0000-0000-000000000002', TRUE,
        '2026-01-01 00:00:00.000', '2026-01-01 00:00:00.000');

-- Tarifas ficticias minimas para poder probar el motor de calculo (Fase 5).
INSERT INTO tariff (id, name, description, vehicle_type_id, modality, value, start_time, end_time,
                     tolerance_minutes, min_time_minutes, time_unit, fraction_minutes, rounding,
                     priority, active, valid_from, valid_to, created_at, updated_at, created_by, updated_by)
VALUES ('00000000-0000-0000-0000-000000000501', 'Por tiempo - Auto', 'Tarifa por fracciones de 60 minutos',
        '00000000-0000-0000-0000-000000000101', 'BY_TIME', 1000, NULL, NULL,
        10, NULL, 'MINUTES', 60, 'UP',
        2, TRUE, '2026-01-01', NULL, '2026-01-01 00:00:00.000', '2026-01-01 00:00:00.000',
        '00000000-0000-0000-0000-000000000401', '00000000-0000-0000-0000-000000000401');

INSERT INTO tariff (id, name, description, vehicle_type_id, modality, value, start_time, end_time,
                     tolerance_minutes, min_time_minutes, time_unit, fraction_minutes, rounding,
                     priority, active, valid_from, valid_to, created_at, updated_at, created_by, updated_by)
VALUES ('00000000-0000-0000-0000-000000000502', 'Todo el día - Moto', 'Tarifa fija de dia completo',
        '00000000-0000-0000-0000-000000000102', 'FULL_DAY', 8000, '06:00:00', '22:00:00',
        10, NULL, NULL, NULL, 'UP',
        1, TRUE, '2026-01-01', NULL, '2026-01-01 00:00:00.000', '2026-01-01 00:00:00.000',
        '00000000-0000-0000-0000-000000000401', '00000000-0000-0000-0000-000000000401');
