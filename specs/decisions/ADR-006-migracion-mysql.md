# ADR-006 — Verificación y procedimiento del cambio SQLite → MySQL (Fase 12)

**Estado:** Aceptado (2026-09-29). Precisa y verifica ADR-002.

## Contexto

`prompt.md` §17 y la Fase 12 exigen comprobar que el proyecto pueda cambiar de SQLite a MySQL **sin modificar la lógica de dominio**, y documentar el procedimiento. ADR-002 dejó preparada la infraestructura (UUID, Flyway con SQL "portable", Spring Data JPA, perfil `production`), pero nunca se había ejecutado contra un MySQL real: el stack `docker-compose.prod.yml` de la Fase 11 solo se validó con `docker compose config`.

## Qué se verificó (con MySQL 8.4.11 real)

| Verificación | Resultado |
|---|---|
| Suite completa del backend (57 pruebas: dominio, motor tarifario, integración HTTP, seguridad, caja, reportes, integridad) sobre MySQL, **sin cambiar código**: solo variables `SPRING_DATASOURCE_*` | 57/57 ✅ (`scripts/verify-mysql.sh`) |
| Misma suite sobre SQLite | 57/57 ✅ (`mvn test`) |
| Stack de producción real (`docker-compose.prod.yml`, perfil `production`) | Arranca; Flyway aplica V1, V3, V3.1; 18 FK y 18 columnas `DATETIME(6)` creadas; credenciales de desarrollo inexistentes (401) |
| Suite E2E de la interfaz (Playwright, 12 escenarios: ingreso → cobro → caja → dashboard → reportes, responsive) contra ese stack con MySQL | 12/12 ✅ (dos corridas; datos semilla cargados **solo** para la verificación) |
| Código Java de producción modificado durante la fase | **Ninguno** (`src/main/java` sin cambios) |

## Hallazgos y decisiones

1. **MySQL descarta en silencio las llaves foráneas "en línea" de V1** (`col CHAR(36) REFERENCES t(id)`): el esquema quedaba sin ninguna FK y aceptaba referencias huérfanas. SQLite sí las aplica. → **Migración exclusiva de MySQL** `db/vendor/mysql/V3_1__mysql_foreign_keys_and_datetime.sql` que las declara como restricciones de tabla (18 FK). No se editó V1: ya está aplicada en las bases SQLite y cambiarla rompería su checksum de Flyway.
2. **`TIMESTAMP` en MySQL** solo llega a 2038-01-19 y, sin precisión, descarta milisegundos. → la misma V3_1 las cambia a `DATETIME(6)` (18 columnas). Los valores siguen siendo instantes UTC (`serverTimezone=UTC`); verificado que los microsegundos se conservan.
3. **Migraciones por motor:** `spring.flyway.locations` incluye `classpath:db/vendor/{vendor}` en los tres perfiles; Spring Boot reemplaza `{vendor}` por el motor detectado (`mysql`, `sqlite`). **Convención de numeración:** las migraciones exclusivas de un motor usan versiones intermedias (`V3_1`) entre dos comunes; las comunes siguen en `db/migration` con la siguiente versión entera (`V4`, …) y deben ser portables. Nunca reutilizar un número en dos ubicaciones.
4. **El perfil `test` abría SQLite sin `foreign_keys=on`** (a diferencia de `development`): las pruebas nunca verificaban la integridad referencial. → activado en la URL; nueva prueba `SchemaIntegrityTest` que exige a la base rechazar una referencia inexistente en ambos motores (habría detectado el hallazgo 1).
5. **El perfil `production` no podía conectarse a MySQL 8 sin SSL** (`Public Key Retrieval is not allowed`, por `caching_sha2_password`): el stack de producción de la Fase 11 nunca arrancaba. → parámetro `DB_ALLOW_PUBLIC_KEY_RETRIEVAL` (por defecto `false`); `docker-compose.prod.yml` lo pone en `true` porque el tráfico va por la red interna de Docker. En un despliegue con MySQL externo, usar `DB_USE_SSL=true` y dejarlo en `false`.
6. **Ambos compose compartían nombre de proyecto** (`parking`, por estar en la misma carpeta): levantar uno reemplazaba los contenedores del otro. → `name: parking-prod` en `docker-compose.prod.yml`.

## Procedimiento: pasar de SQLite a MySQL

Requisitos: MySQL 8.x (verificado con 8.4), base de datos vacía y un usuario con permisos DDL sobre ella (Flyway crea las tablas).

**Opción A — con Docker (MySQL incluido):**

```bash
cp .env.example .env
# Editar .env: DB_USER, DB_PASSWORD (fuerte), CORS_ALLOWED_ORIGINS=<URL pública del frontend>,
#   PARKING_BOOTSTRAP_ADMIN_USERNAME y PARKING_BOOTSTRAP_ADMIN_PASSWORD (primer administrador, ADR-007).
# Con el MySQL del compose: DB_USE_SSL=false (el compose ya activa DB_ALLOW_PUBLIC_KEY_RETRIEVAL).
docker compose -f docker-compose.prod.yml up --build -d
docker compose -f docker-compose.prod.yml ps        # los tres servicios "healthy"
```

**Opción B — backend contra un MySQL existente:**

```bash
mvn -f backend/pom.xml -DskipTests package   # genera backend/target/parking-backend-<versión>.jar
export SPRING_PROFILES_ACTIVE=production
export DB_HOST=<host> DB_PORT=3306 DB_NAME=parking DB_USER=<usuario> DB_PASSWORD=<clave>
export DB_USE_SSL=true                     # recomendado fuera de una red privada
export CORS_ALLOWED_ORIGINS=<URL pública del frontend> PARKING_TIMEZONE=America/Bogota
export PARKING_BOOTSTRAP_ADMIN_USERNAME=<usuario> PARKING_BOOTSTRAP_ADMIN_PASSWORD=<clave de 8+ caracteres>
java -jar backend/target/parking-backend-*.jar
```

Al arrancar, Flyway crea el esquema completo y los datos de referencia (V1, V3, la exclusiva de MySQL V3.1 y V4), y la aplicación crea el primer administrador. No hay que ejecutar SQL a mano ni cambiar código. Después: iniciar sesión con ese administrador, retirar `PARKING_BOOTSTRAP_ADMIN_PASSWORD` del entorno, cambiar la contraseña, y crear las tarifas y completar la configuración.

**Comprobaciones posteriores:**

1. `GET /actuator/health` → `{"status":"UP"}`.
2. En MySQL: `SELECT version, success FROM flyway_schema_history;` → V1, V3, V3.1 y V4 con `success = 1`.
3. Integridad: `SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = '<DB_NAME>';` → 18.
4. El log del backend muestra `Administrador inicial '<usuario>' creado`, y ese usuario puede iniciar sesión.

**Para repetir la verificación del código contra MySQL** (por ejemplo antes de cada entrega o en CI): `bash scripts/verify-mysql.sh`.

## Pendiente (decisión del usuario)

- ~~Arranque de producción sin datos~~ **Resuelto en ADR-007:** los datos de referencia se cargan con la migración común V4, y el primer administrador se crea al arrancar desde variables de entorno.
- **Migración de datos existentes:** si en algún momento hay operación real sobre SQLite que deba conservarse, se necesitará un procedimiento de copia de datos (hoy las bases SQLite solo contienen datos de desarrollo y prueba).

## Consecuencias

- El cambio de motor es solo de configuración para la aplicación. Las diferencias de motor quedan encapsuladas en `db/vendor/{motor}`, y el dominio y la aplicación no saben qué base usan.
- Cada migración común nueva debe probarse en ambos motores (`mvn test` y `scripts/verify-mysql.sh`). Ejemplo real: la V5 (2026-09-29) pasaba en SQLite y fallaba en MySQL porque usaba una columna `last_value`, que es palabra reservada en MySQL 8.
- **Concurrencia:** SQLite serializa las transacciones de escritura (`transaction_mode=IMMEDIATE`) y MySQL no, así que una condición de carrera puede quedar oculta en desarrollo. Así ocurrió con el número de operación de los pagos (`COUNT + 1`): con dos cobros simultáneos en MySQL, uno fallaba con 500. Lo detectó la suite E2E corrida contra el stack de producción; corregido con la V5 (contador atómico) y cubierto por la prueba de cobros simultáneos de `ParkingOperationsIntegrationTest`, que solo reproduce el problema cuando corre sobre MySQL.
- Primer arranque en frío del backend en un equipo con poca memoria: las primeras peticiones (BCrypt, JIT, Hibernate) pueden tardar varios segundos; no afecta el funcionamiento.
