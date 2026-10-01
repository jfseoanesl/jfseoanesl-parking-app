# ADR-002 — Persistencia inicial en SQLite con migración futura a MySQL

**Estado:** Aceptado (2026-09-22)

## Contexto

`prompt.md` §16-17 exige iniciar con SQLite persistente en archivo (no en memoria para operación real) y dejar preparada la migración a MySQL sin reescribir la lógica de dominio.

## Problema

Definir el mecanismo concreto (identificadores, migraciones, dialectos, perfiles) que garantice que el cambio de motor sea solo de configuración.

## Alternativas consideradas

- **IDs:** autoincremental nativo del motor vs. UUID generado en aplicación.
- **Migraciones:** scripts manuales por entorno vs. herramienta de migración versionada (Flyway/Liquibase).
- **Acceso a datos:** SQL nativo por caso de uso vs. Spring Data JPA + Repository Pattern.

## Decisión

- IDs: **UUID v4** generado en la capa de aplicación/dominio antes de persistir, almacenado como `CHAR(36)` — independiente del motor (prompt.md §24).
- Migraciones: **Flyway**, con scripts SQL escritos en sintaxis portable entre SQLite y MySQL (evitando `AUTOINCREMENT`, `ROWID`, `ENUM` nativo).
- Acceso a datos: **Spring Data JPA** sobre interfaces de repositorio definidas en `Domain`, implementadas en `Infrastructure`.
- Perfiles: `development` (SQLite archivo `parking.db`), `test` (SQLite en memoria, `jdbc:sqlite::memory:`, solo para pruebas automatizadas), `production` (MySQL).

## Justificación

Los autoincrementales de SQLite y MySQL no son compatibles entre sí en todos los escenarios (tipos, comportamiento en réplicas futuras); usar UUID generado en aplicación elimina esa dependencia por completo y permite generar el ID antes del `INSERT` (útil para devolver el recurso creado o para idempotencia). Flyway con SQL portable es la opción más simple que cumple el requisito explícito de "no reescritura de lógica funcional" al migrar.

## Consecuencias

- Los UUID como texto ocupan más espacio e indexan más lento que enteros — aceptable para el volumen esperado de un parqueadero pequeño (cientos de operaciones/día, no millones).
- Debe validarse en Fase 4 que el dialecto Hibernate para SQLite (comunidad, no oficial de Hibernate) soporte todas las construcciones usadas; si aparecen limitaciones no triviales, se documentará una nueva decisión.
- La verificación real de "cambiar de motor sin tocar dominio" se ejecutó en la Fase 12: ver `ADR-006-migracion-mysql.md` (suite completa 57/57 y E2E 12/12 sobre MySQL 8.4, sin cambios de código Java; dos incompatibilidades de SQL "portable" corregidas con una migración exclusiva de MySQL).
