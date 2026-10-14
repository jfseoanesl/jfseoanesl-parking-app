# ADR-009 — Respaldo completo de la base de datos (exportar e importar)

**Estado:** Aceptado (2026-10-06). Implementa RF-073, a pedido del usuario.

## Contexto

El administrador necesita exportar **toda** la información de la aplicación y luego importarla en otra ejecución: otra instalación u otro equipo, una restauración tras una falla, o el paso del perfil de desarrollo (SQLite) al de producción (MySQL, ADR-006). Decisiones del usuario: la importación **reemplaza todo**, el archivo es **JSON portable** y va **sin cifrar**, con una advertencia de que contiene datos sensibles.

## Decisión

- **Endpoints** (solo ADMIN; protegidos por URL `/api/admin/**` en `SecurityConfig` y además con `@PreAuthorize`, para que un no administrador reciba 403 antes de que se lea el cuerpo):
  - `GET /api/admin/backup` descarga `parking-respaldo_<yyyy-MM-dd_HHmm>.json` (`Content-Disposition: attachment`).
  - `POST /api/admin/backup/import`, con el JSON completo en el cuerpo, reemplaza todos los datos y responde `{schemaVersion, counts}`.
- **Formato** (`BackupFile`): `format: "parking-backup"`, `formatVersion: 1`, `schemaVersion` (versión Flyway), `exportedAt`, `exportedBy`, `sourceDatabase`, `counts` y `tables`. `tables` lleva una lista por tabla, en orden de llaves foráneas: roles, users, vehicleTypes, paymentMethods, configuration, tariffs, cashSessions, parkingSessions, payments, cashMovements, auditLogs y operationCounters.
- **Portabilidad:** las filas se leen y se escriben a través de las **entidades JPA** existentes. Así, cada motor convierte sus propios tipos (`Instant`, `DECIMAL`, booleanos) y el JSON queda neutro. `operation_counter` (consecutivos OP e ING) se maneja con `JdbcTemplate`. La columna generada `plate_active` no se exporta porque la recalcula la base de datos.
- **Importación atómica** (`DatabaseBackupService.importReplacing`, `@Transactional`):
  1. Valida, antes de tocar datos: formato, versión del formato, **misma versión de esquema**, que estén todas las tablas, que haya **al menos un administrador activo** (si no, la instalación quedaría sin acceso) y que las contraseñas sean hashes BCrypt.
  2. Borra las tablas en orden inverso de llaves foráneas (bulk delete) y limpia el contexto de persistencia.
  3. Inserta en orden de llaves foráneas, con un flush por tabla, y restaura los contadores.
  
  Si algo falla, se hace rollback y la base queda intacta.
- **Versión del esquema** leída de `flyway_schema_history` con la conexión de la transacción en curso. `Flyway#info()` abre otra conexión, y con el pool de una sola conexión de SQLite eso se bloqueaba.
- **Auditoría:** `DATABASE_EXPORTED` al exportar. `DATABASE_IMPORTED` al importar, solo si el administrador que importa existe en los datos restaurados, porque `audit_log.user_id` es una llave foránea a `app_user`. Si no existe, la importación queda en el log de la aplicación con nivel WARN.
- **Sesiones:** tras importar, `ActiveUserSessionFilter` invalida las sesiones de usuarios que ya no existen. El frontend cierra la sesión y lleva al login.
- **Tamaño:** nginx acepta hasta 50 MB en `/api/`; su límite por defecto, 1 MB, no alcanza para un respaldo real.
- **Frontend** (`/respaldo`, menú "Respaldo"):
  - **Descargar respaldo**, con una advertencia de datos sensibles.
  - **Importar:** se elige el archivo y antes de confirmar se muestra un resumen leído en el navegador (fecha, motor de origen, versión y conteos). La confirmación exige escribir `REEMPLAZAR`.

## Alternativas descartadas

- **Volcado SQL nativo** (`mysqldump`, `.dump` de SQLite): solo sirve entre instalaciones del mismo motor, depende de herramientas externas en el contenedor y es difícil de validar antes de aplicarlo.
- **Combinar con los datos existentes:** genera conflictos de identidad y de reglas de negocio (placas activas duplicadas, consecutivos OP/ING repetidos, cajas abiertas dobles). Reemplazar todo es predecible.
- **Cifrado con contraseña:** se pospuso por decisión del usuario. Las contraseñas ya viajan como hash BCrypt.

## Consecuencias

- Un respaldo solo se importa en una instalación con la **misma versión de esquema**. Para pasar datos a una versión más nueva, se actualiza primero la instalación de origen, se exporta y luego se importa.
- El archivo contiene datos sensibles (usuarios, cobros y auditoría). La responsabilidad de custodiarlo queda en el administrador, y la UI lo advierte.
- Todo se arma en memoria: es adecuado para el volumen de un parqueadero (unos 170 KB con 150 sesiones). Si el volumen creciera mucho, habría que generar el archivo por partes, sin cargarlo entero.
- Verificado:
  - `BackupIntegrationTest`: ida y vuelta con los contadores restaurados, rechazos sin tocar los datos, permisos 401/403 y auditoría. Suite 98/98 en SQLite y MySQL.
  - E2E de descarga y resumen.
  - **Transferencia real** de una instalación SQLite (8081) a una MySQL temporal: 148 sesiones, 86 pagos y 42 registros de auditoría con conteos y montos idénticos, login con los usuarios del respaldo, rechazo del administrador anterior, y el siguiente ingreso recibió el ING esperado.
