Migraciones exclusivas de SQLite (Flyway `classpath:db/vendor/{vendor}`). Hoy no hay ninguna:
el esquema común de `db/migration` ya se comporta correctamente en SQLite. Ver
`db/vendor/mysql/` y `specs/decisions/ADR-006-migracion-mysql.md` para la convención de numeración.
