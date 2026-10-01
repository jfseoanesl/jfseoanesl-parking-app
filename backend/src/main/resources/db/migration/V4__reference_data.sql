-- Datos de referencia sin los que la aplicacion no puede operar (ADR-007). A diferencia de
-- db/seed/V2 (solo development/test: incluye credenciales y tarifas de prueba), esta migracion
-- corre en TODOS los perfiles, incluido production: roles, tipos de vehiculo, medios de pago
-- (BR-018) y una configuracion inicial editable. Nunca usuarios ni contraseñas: el primer
-- administrador se crea al arrancar desde variables de entorno (AdminBootstrap). Tampoco tarifas:
-- son precios del negocio y las define el administrador.
--
-- Idempotente y portable (SQLite y MySQL): INSERT ... SELECT desde una tabla derivada con
-- WHERE NOT EXISTS por codigo. En las bases que ya aplicaron V2 no inserta nada.

INSERT INTO role (id, code, name)
SELECT v.id, v.code, v.name
FROM (SELECT '00000000-0000-0000-0000-000000000001' AS id, 'ADMIN' AS code, 'Administrador' AS name) v
WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.code = v.code);

INSERT INTO role (id, code, name)
SELECT v.id, v.code, v.name
FROM (SELECT '00000000-0000-0000-0000-000000000002' AS id, 'EMPLOYEE' AS code, 'Empleado' AS name) v
WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.code = v.code);

INSERT INTO vehicle_type (id, code, name, active)
SELECT v.id, v.code, v.name, TRUE
FROM (SELECT '00000000-0000-0000-0000-000000000101' AS id, 'CAR' AS code, 'Automóvil' AS name) v
WHERE NOT EXISTS (SELECT 1 FROM vehicle_type t WHERE t.code = v.code);

INSERT INTO vehicle_type (id, code, name, active)
SELECT v.id, v.code, v.name, TRUE
FROM (SELECT '00000000-0000-0000-0000-000000000102' AS id, 'MOTORCYCLE' AS code, 'Motocicleta' AS name) v
WHERE NOT EXISTS (SELECT 1 FROM vehicle_type t WHERE t.code = v.code);

INSERT INTO payment_method (id, code, name, active, category)
SELECT v.id, v.code, v.name, TRUE, v.category
FROM (SELECT '00000000-0000-0000-0000-000000000201' AS id, 'CASH' AS code, 'Efectivo' AS name, 'CASH' AS category) v
WHERE NOT EXISTS (SELECT 1 FROM payment_method p WHERE p.code = v.code);

INSERT INTO payment_method (id, code, name, active, category)
SELECT v.id, v.code, v.name, TRUE, v.category
FROM (SELECT '00000000-0000-0000-0000-000000000202' AS id, 'TRANSFER' AS code, 'Transferencia' AS name, 'ELECTRONIC' AS category) v
WHERE NOT EXISTS (SELECT 1 FROM payment_method p WHERE p.code = v.code);

INSERT INTO payment_method (id, code, name, active, category)
SELECT v.id, v.code, v.name, TRUE, v.category
FROM (SELECT '00000000-0000-0000-0000-000000000203' AS id, 'OTHER' AS code, 'Otros' AS name, 'OTHER' AS category) v
WHERE NOT EXISTS (SELECT 1 FROM payment_method p WHERE p.code = v.code);

-- Configuracion unica: nombre generico y sin capacidad; el administrador la completa en Configuracion.
INSERT INTO configuration (id, parking_name, timezone, currency, updated_at)
SELECT v.id, v.parking_name, 'America/Bogota', 'COP', '2026-01-01 00:00:00.000'
FROM (SELECT '00000000-0000-0000-0000-000000000301' AS id, 'Mi parqueadero' AS parking_name) v
WHERE NOT EXISTS (SELECT 1 FROM configuration);
