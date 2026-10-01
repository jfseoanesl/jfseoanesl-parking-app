# 06 — Arquitectura

> Fase 2 — SDD. Arquitectura lógica, diagrama de componentes, arquitectura PWA y estrategia de migración SQLite → MySQL. Aplica los principios de `prompt.md` §18 (Clean Architecture sin sobreingeniería) y §41 (correctitud > simplicidad > ... > extensibilidad).

## Arquitectura lógica en capas

```
┌─────────────────────────────────────────────────────────┐
│  Presentation (React + TS + Vite + Tailwind)             │
│  Páginas, componentes, cliente HTTP, service worker       │
└───────────────────────────┬─────────────────────────────┘
                             │ REST/JSON (ver 08-api-contract.md)
┌───────────────────────────▼─────────────────────────────┐
│  Application (Spring Boot — capa de servicios)            │
│  Casos de uso, DTOs, orquestación, validación de entrada  │
└───────────────────────────┬─────────────────────────────┘
┌───────────────────────────▼─────────────────────────────┐
│  Domain                                                   │
│  Entidades, reglas de negocio, motor de tarifas,          │
│  interfaces de repositorio (puertos), servicio de tiempo  │
└───────────────────────────┬─────────────────────────────┘
┌───────────────────────────▼─────────────────────────────┐
│  Infrastructure                                            │
│  JPA (Spring Data), adaptadores SQLite/MySQL, seguridad    │
│  (Spring Security), logging, Flyway                        │
└─────────────────────────────────────────────────────────┘
```

Regla de dependencia: las capas superiores dependen de las inferiores, nunca al revés. **Domain no depende de Spring, JPA ni de ningún framework** — solo expone interfaces (`ParkingSessionRepository`, `TariffRepository`, etc.) que `Infrastructure` implementa. Esto es lo que permite migrar SQLite → MySQL sin tocar `Domain` ni `Application` (RNF-016).

## Paquetes propuestos (backend)

```
com.parking
 ├── presentation        (controllers REST, DTOs de entrada/salida, mappers)
 ├── application          
 │    ├── auth            (AuthService)
 │    ├── parking          (ParkingEntryService, ParkingExitService)
 │    ├── tariff           (TariffAdminService)
 │    ├── payment          (PaymentService)
 │    ├── cash             (CashSessionService)
 │    ├── report           (ReportService)
 │    ├── user             (UserService)
 │    └── audit            (AuditService)
 ├── domain
 │    ├── model            (entidades: User, ParkingSession, Tariff, Payment, CashSession, ...)
 │    ├── repository       (interfaces/puertos)
 │    ├── tariffengine      (TariffStrategy, TariffStrategyFactory, TariffPriorityResolver)
 │    └── time              (TimeProvider — servicio de tiempo, BR-026)
 └── infrastructure
      ├── persistence       (entidades JPA o mapeo, implementaciones de repositorio)
      ├── security           (Spring Security, filtros, hashing)
      ├── config             (perfiles, datasource, CORS, OpenAPI)
      └── migration          (scripts Flyway)
```

## Motor de tarifas (Strategy + Factory + Chain of Responsibility)

```
TariffCalculationService
        │
        ▼
TariffStrategyFactory.resolve(vehicleType, entryTime, exitTime)
        │
        ├─► busca tarifas activas y vigentes (TariffRepository)
        ▼
TariffPriorityChain
        │  (ordena candidatas por `priority`; en empate, evalúa todas y
        │   selecciona el menor cobro resultante — BR-008)
        ▼
TariffStrategy (una implementación por `modality`):
        - FullAmTariffStrategy
        - FullPmTariffStrategy
        - FullDayTariffStrategy
        - ByTimeTariffStrategy
        │
        ▼
   TariffCalculationResult { tariffId, amount, breakdown }
```

Cada `TariffStrategy` implementa una interfaz común `calculate(ParkingSession, Tariff): TariffCalculationResult`. Agregar una modalidad futura (nocturna, festiva) significa **agregar una clase nueva**, no modificar las existentes (Open/Closed Principle, RNF-023). El manejo de cruce de medianoche (BR-014) y permanencias >24h (BR-015) vive en `ByTimeTariffStrategy` y en una utilidad de dominio compartida (`StayDurationCalculator`), no duplicado por estrategia.

## Componentes — diagrama de alto nivel

```
┌───────────────┐      HTTPS/JSON      ┌─────────────────────┐
│  React PWA     │◄────────────────────►│  Spring Boot API     │
│  (Vite build)  │                      │  Controllers          │
│  Service Worker│                      └──────────┬────────────┘
│  (Workbox)     │                                 │
└───────────────┘                                 ▼
                                        ┌─────────────────────┐
                                        │  Application Services│
                                        └──────────┬────────────┘
                                                   ▼
                                        ┌─────────────────────┐
                                        │  Domain (motor de    │
                                        │  tarifas, entidades) │
                                        └──────────┬────────────┘
                                                   ▼
                                        ┌─────────────────────┐
                                        │  Spring Data JPA      │
                                        │  Repositorios          │
                                        └──────────┬────────────┘
                                                   ▼
                                   ┌───────────────────────────────┐
                                   │  SQLite (dev) / MySQL (prod)    │
                                   │  Flyway migrations               │
                                   └───────────────────────────────┘
```

## Arquitectura PWA

| Aspecto | Decisión |
|---|---|
| Manifest | `manifest.webmanifest` generado por `vite-plugin-pwa`, con iconos 192/512, `display: standalone`, `theme_color`/`background_color` configurables. |
| Service worker | Generado por Workbox vía `vite-plugin-pwa` (estrategia `generateSW` para el MVP; se puede migrar a `injectManifest` si se necesita lógica de cache custom). |
| Cache de recursos estáticos | Estrategia **cache-first** con revalidación en background (`StaleWhileRevalidate`) para JS/CSS/imágenes/fuentes — cumple RF-069/RF-070, BR-019. |
| Llamadas a la API (datos transaccionales) | Estrategia **network-only** (sin cache, sin cola offline) — cumple BR-019: ninguna operación de ingreso/salida/cobro/caja se ejecuta sin conexión en el MVP. Un fallo de red en estas llamadas muestra un mensaje claro ("Sin conexión, intenta de nuevo") en vez de encolar silenciosamente. |
| Actualización de versión | `registerType: 'autoUpdate'` con notificación simple al usuario cuando hay una nueva versión del shell disponible. |
| Instalación | Prompt de instalación estándar del navegador (`beforeinstallprompt` en Android/desktop); en iOS se documenta el flujo manual "Compartir → Agregar a inicio" dado que Safari no dispara ese evento. |

Separación explícita (prompt.md §19): **offline de recursos estáticos** (implementado en el MVP) vs. **offline transaccional de datos** (fuera de alcance MVP, documentado en `00-product-vision.md` §3; la arquitectura de capas y el uso de DTOs en `Application` dejan espacio para introducir una cola de sincronización en el cliente más adelante sin rediseñar el dominio).

## Estrategia SQLite → MySQL

| Elemento | Desarrollo (`development`) | Producción futura (`production`) |
|---|---|---|
| Motor | SQLite, archivo `parking.db` (no en memoria — RNF-014) | MySQL 8.x |
| Datasource | `jdbc:sqlite:./data/parking.db` | `jdbc:mysql://<host>:3306/parking` |
| Dialecto JPA | Dialecto SQLite (community, ej. `org.hibernate.community.dialect.SQLiteDialect`) | `org.hibernate.dialect.MySQLDialect` |
| Migraciones | Flyway, scripts SQL escritos en sintaxis portable (evitar `AUTOINCREMENT`/`ROWID` de SQLite y `ENUM` nativo de MySQL — ver `07-data-model.md`) | Los mismos scripts Flyway, ejecutados contra MySQL |
| IDs | UUID generado en `Domain`/`Application` antes de persistir (no autoincremental de motor) | Igual — el ID nunca depende del motor |
| Cambio de perfil | `spring.profiles.active=development` | `spring.profiles.active=production` |

**Lo que la migración futura SÍ requiere (prompt.md §17):** configuración de datasource, driver MySQL, ejecución de las mismas migraciones Flyway contra la base vacía o de una migración de datos histórica (fuera de alcance MVP, documentada como tarea de Fase 12).
**Lo que NO requiere:** cambios en `Domain`, `Application` ni en las consultas JPQL/Criteria de los repositorios (se evita HQL/SQL nativo específico de un motor salvo excepción justificada y documentada).

## Perfiles

- `development` — SQLite, logging detallado, datos semilla activos, CORS abierto a `localhost`.
- `test` — SQLite en memoria (`jdbc:sqlite::memory:`) para velocidad de pruebas automatizadas (permitido explícitamente solo para este perfil, prompt.md §16).
- `production` — MySQL, logging restringido, sin datos semilla, CORS restringido al dominio real, variables sensibles vía `.env`/variables de entorno (nunca hardcodeadas).

## Decisiones registradas como ADR

- `decisions/ADR-001-stack-tecnologico.md`
- `decisions/ADR-002-persistencia-sqlite.md`
- `decisions/ADR-003-motor-tarifario.md`
- `decisions/ADR-004-autenticacion.md`
