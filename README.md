# Parqueadero

Aplicación web progresiva (PWA) para administrar un parqueadero: ingreso y salida de vehículos, cobro con tarifas configurables, caja con arqueo, mensualidades, reportes, auditoría y respaldo. Funciona en escritorio y en celular, y se puede instalar como app.

[![CI/CD](https://github.com/jfseoanesl/jfseoanesl-parking-app/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/jfseoanesl/jfseoanesl-parking-app/actions/workflows/ci-cd.yml)
[![Docker Hub backend](https://img.shields.io/docker/v/jfseoanesl/parking-backend?label=parking-backend&logo=docker)](https://hub.docker.com/r/jfseoanesl/parking-backend)
[![Docker Hub frontend](https://img.shields.io/docker/v/jfseoanesl/parking-frontend?label=parking-frontend&logo=docker)](https://hub.docker.com/r/jfseoanesl/parking-frontend)

El proyecto se desarrolló con *Spec-Driven Development*. La especificación completa (requisitos, reglas de negocio, casos de uso, modelo de datos, API, UX, seguridad y decisiones de arquitectura) está en [`specs/`](specs/).

---

## Contenido

- [Funcionalidades](#funcionalidades)
- [Tecnologías](#tecnologías)
- [Inicio rápido con Docker](#inicio-rápido-con-docker)
- [Producción](#producción)
- [Desarrollo local](#desarrollo-local)
- [Pruebas](#pruebas)
- [CI/CD](#cicd-github-actions--docker-hub)
- [Configuración](#configuración-variables-de-entorno)
- [Seguridad](#seguridad)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Documentación](#documentación)

---

## Funcionalidades

**Operación diaria (empleado y administrador)**
- **Ingreso de vehículos:** la placa se normaliza al escribir (mayúsculas, sin espacios) y no puede haber dos sesiones activas con la misma placa. Cada ingreso recibe un consecutivo **`ING-000001`** y se puede imprimir un **comprobante de ingreso** para el cliente.
- **Vehículos parqueados:** búsqueda por placa, filtro por tipo de vehículo y orden por hora de ingreso o tiempo de permanencia.
- **Salida y cobro:**
  - el valor lo calcula el motor de tarifas;
  - se puede elegir una **tarifa alterna aplicable**, y ese cambio queda auditado;
  - el pago queda registrado con un consecutivo **`OP-000001`** y un comprobante imprimible.
- **Caja:**
  - apertura con saldo inicial y operaciones del turno;
  - cierre con **arqueo**: efectivo esperado contra efectivo contado, con la diferencia. Solo el efectivo cuenta como físico.
- **Mensualidades:**
  - cada cliente mensual tiene un valor y un día de corte pactados y **placas autorizadas**; solo una puede estar dentro a la vez;
  - el ingreso de una placa autorizada se reconoce automáticamente;
  - la salida es **sin cobro** mientras la mensualidad esté al día o en días de gracia; vencida, se cobra con la tarifa normal y la app lo advierte;
  - el pago mensual se cobra en caja y genera comprobante.

**Administración**
- **Dashboard:** ocupación frente a capacidad, ingresos, salidas y **recaudo de hoy** (incluye mensualidades), cajas abiertas y últimos movimientos.
- **Tarifas:**
  - por tiempo (fracción y redondeo), mañana, tarde o día completo, con tolerancia, tiempo mínimo, vigencia y prioridad;
  - cada cambio se audita con el valor anterior y el nuevo.
- **Usuarios** (administrador y empleado): alta, activación, cambio de contraseña propia y de otros.
- **Configuración:**
  - datos del parqueadero, capacidad, zona horaria y moneda;
  - formato del comprobante y si se ofrece imprimir al ingreso y/o a la salida;
  - días de gracia de las mensualidades;
  - catálogos de tipos de vehículo y medios de pago.
- **Historial** de sesiones con filtros por fecha, placa, estado y empleado, y reimpresión de comprobantes.
- **Correcciones de caja** sobre cierres ya finalizados (con motivo, auditadas) y **anulación** de ingresos (con motivo, auditada).
- **Reportes:**
  - **General:** diario, semanal, mensual o por rango. Vehículos ingresados, finalizados y anulados; recaudo por medio de pago, tarifa y empleado; duración promedio; hora pico; diferencias de caja.
  - **Mensualidades:** estado de cartera con **días de mora**, detalle de pagos y cartera vencida estimada. Filtros por período, varios estados a la vez y cliente/documento/placa.
  - Ambos se exportan a **Excel, PDF y CSV**.
- **Auditoría** de cambios administrativos y operaciones sensibles, con filtros por entidad, usuario y fechas.
- **Respaldo:**
  - exporta toda la base de datos a un **JSON portable**;
  - se importa en otra instalación con la misma versión, incluso de SQLite a MySQL;
  - la importación es atómica y reemplaza todos los datos.

**Plataforma**
- **PWA instalable**, con la interfaz disponible ante pérdidas momentáneas de conexión. La API nunca se cachea.
- **Diseño responsive:** barra lateral en escritorio y barra inferior en móvil.
- **Dos motores de base de datos con el mismo código:** SQLite (desarrollo o una sola sede) y MySQL 8.4 (producción).

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1 (Web MVC, Security, Data JPA), Flyway, Hibernate; Apache POI y OpenPDF para exportar |
| Base de datos | SQLite (desarrollo, pruebas) · MySQL 8.4 (producción) |
| Frontend | React 19, TypeScript, Vite 6, Tailwind CSS 4, React Router 7, Lucide, vite-plugin-pwa (Workbox) |
| Pruebas | JUnit 5 + MockMvc, Vitest + Testing Library, Playwright (E2E y responsive) |
| Contenedores | Docker multi-etapa: backend en JRE 21 Alpine; frontend en nginx 1.27 Alpine, que también hace de proxy de `/api` |
| CI/CD | GitHub Actions → Docker Hub |

Arquitectura del backend por capas (dominio, aplicación, infraestructura y presentación), con puertos de repositorio. Ver [`specs/06-architecture.md`](specs/06-architecture.md).

## Inicio rápido con Docker

Requiere Docker y Docker Compose.

```bash
docker compose up --build -d
```

- App: **http://localhost:8081**
- Backend (health): http://localhost:18080/actuator/health

Este stack usa SQLite. Los datos se guardan en el volumen `parking_backend_data` y se conservan con `docker compose down` siempre que no se use `-v`.

Usuarios de prueba, **solo para desarrollo** (nunca en producción):

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | Administrador |
| `empleado` | `empleado123` | Empleado |

## Producción

El stack de producción usa MySQL 8.4, no carga datos ni credenciales de prueba y puede correr junto al de desarrollo, porque usa otros puertos y otro nombre de proyecto.

```bash
cp .env.example .env   # completar DB_*, CORS_ALLOWED_ORIGINS y PARKING_BOOTSTRAP_ADMIN_*
docker compose -f docker-compose.prod.yml up --build -d
```

- App: **http://localhost:8091**
- Backend (health): http://localhost:18090/actuator/health
- MySQL: `localhost:13306`

**Primer arranque:**
1. Los datos de referencia (roles, tipos de vehículo, medios de pago y configuración inicial) los crea una migración.
2. El **primer administrador** se crea a partir de `PARKING_BOOTSTRAP_ADMIN_USERNAME` y `PARKING_BOOTSTRAP_ADMIN_PASSWORD` (mínimo 8 caracteres).
3. Después del primer arranque, **quita la contraseña del `.env`** y cámbiala desde la aplicación.
4. Crea las tarifas y completa la configuración.

Ver [ADR-007](specs/decisions/ADR-007-arranque-produccion.md).

**Desplegar con las imágenes de Docker Hub**, sin compilar en el servidor. Se usa el mismo `.env`:

```bash
export IMAGE_TAG=latest   # o una versión (1.2.0) o un commit (sha-<commit>)
docker compose -f docker-compose.prod.yml -f docker-compose.hub.yml pull
docker compose -f docker-compose.prod.yml -f docker-compose.hub.yml up -d --no-build
```

**HTTPS:** al servir la app con HTTPS, define `SESSION_COOKIE_SECURE=true`. HTTPS también es necesario para instalar la PWA en el celular.

**Requisitos de la VM:**
- **Mínimo:** 1–2 vCPU, 2 GB de RAM y 20 GB de SSD.
- **Recomendado:** 2 vCPU, 4 GB de RAM y 30–40 GB de SSD.
- **Sistema:** Linux **x86_64**. Las imágenes se publican para `linux/amd64`, así que no sirven las instancias ARM.

En reposo, el backend usa cerca de 425 MB, MySQL cerca de 485 MB y nginx cerca de 10 MB.

**VM pequeña (~1 GB, p. ej. GCP e2-micro de la capa gratuita):** usa `docker-compose.small.yml`. Este archivo:
- limita el heap de la JVM, el buffer de MySQL, los hilos y las conexiones;
- fija un tope de memoria por contenedor;
- da al backend hasta 5 minutos para el primer arranque.

**Antes, crea 2 GB de swap.** Sin swap, el sistema mata el backend por falta de memoria (OOM).

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

docker compose -f docker-compose.prod.yml -f docker-compose.hub.yml -f docker-compose.small.yml pull
docker compose -f docker-compose.prod.yml -f docker-compose.hub.yml -f docker-compose.small.yml up -d --no-build
```

Funciona para uso ligero (un parqueadero, pocos usuarios), pero arranca lento y los reportes o respaldos grandes tardan. No compiles en una VM de 1 GB: usa las imágenes de Docker Hub.

**Pasar datos de una instalación a otra** (por ejemplo, de SQLite a MySQL):
1. En el origen: **Respaldo → Descargar respaldo**.
2. En el destino: **Respaldo → Importar**, revisar el resumen y confirmar escribiendo `REEMPLAZAR`.

Las dos instalaciones deben tener la misma versión de la aplicación. Ver [ADR-009](specs/decisions/ADR-009-respaldo-base-de-datos.md).

## Desarrollo local

**Backend** (requiere Java 21 y Maven):

```bash
cd backend
mvn spring-boot:run
```

Perfil `development`: SQLite en `backend/data/parking.db` (se crea solo). Las migraciones y los datos de prueba se aplican al arrancar. API en http://localhost:8080.

**Frontend** (requiere Node 22):

```bash
cd frontend
npm install
npm run dev
```

App en http://localhost:5173. El servidor de desarrollo reenvía `/api/**` al backend en el puerto 8080, en el mismo origen, así que no hay problemas de CORS ni de cookies.

## Pruebas

| Qué | Comando | Estado actual |
|---|---|---|
| Backend sobre SQLite | `cd backend && mvn test` | 109 pruebas |
| Backend sobre MySQL 8.4 (Docker) | `bash scripts/verify-mysql.sh` | 109 pruebas |
| Frontend (unitarias y componentes) | `cd frontend && npm run test` | 30 pruebas |
| Frontend (tipos y build) | `cd frontend && npx tsc -b && npm run build` | — |
| E2E y responsive (desktop + Pixel 7) | `cd frontend && npm run test:e2e` | 24 escenarios |

- **Pruebas del backend:** comparten una base en memoria y son independientes del orden de ejecución (verificado en orden alfabético, inverso y aleatorio).
- **E2E:** inician sesión como `admin/admin123` y necesitan el backend y el frontend corriendo.
  - Contra el stack Docker: `E2E_BASE_URL=http://localhost:8081 npm run test:e2e`.
  - Con un administrador distinto: `E2E_ADMIN_USERNAME` y `E2E_ADMIN_PASSWORD`.
  - Para usar el Chrome instalado en lugar del Chromium de Playwright: `E2E_BROWSER_CHANNEL=chrome`.

## CI/CD (GitHub Actions → Docker Hub)

El workflow [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) hace dos cosas:

1. **Pruebas en paralelo:** backend sobre SQLite, backend sobre MySQL 8.4, y frontend (tipos, Vitest y build).
2. **Imágenes**, solo si todo pasa: [`jfseoanesl/parking-backend`](https://hub.docker.com/r/jfseoanesl/parking-backend) y [`jfseoanesl/parking-frontend`](https://hub.docker.com/r/jfseoanesl/parking-frontend).

| Evento | Qué se publica en Docker Hub |
|---|---|
| Pull request a `master` | Nada; solo se construyen las imágenes para validar los Dockerfiles |
| Push a `master` | `:latest` y `:sha-<commit>` |
| Tag `vX.Y.Z` (`git tag v1.2.0 && git push origin v1.2.0`) | `:1.2.0`, `:1.2` y `:latest` |
| Ejecución manual (Actions → CI/CD → *Run workflow*) | `:sha-<commit>` |

**Configuración:**
- El namespace `jfseoanesl` está fijo en el workflow.
- Solo hace falta el secreto **`DOCKERHUB_TOKEN`**, en *Settings → Secrets and variables → Actions*: un Access Token de Docker Hub con permiso *Read & Write*. Nunca uses la contraseña.

Ver [ADR-011](specs/decisions/ADR-011-ci-cd.md).

## Configuración (variables de entorno)

En desarrollo no se necesita ninguna. En producción van en `.env` (plantilla: [`.env.example`](.env.example)):

| Variable | Uso |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexión a MySQL |
| `DB_USE_SSL`, `DB_ALLOW_PUBLIC_KEY_RETRIEVAL` | TLS hacia MySQL; la segunda solo cuando no se usa SSL y en una red privada |
| `PARKING_TIMEZONE` | Zona horaria del parqueadero (por defecto `America/Bogota`) |
| `CORS_ALLOWED_ORIGINS` | Origen(es) permitidos del frontend |
| `PARKING_BOOTSTRAP_ADMIN_USERNAME` / `_FULL_NAME` / `_PASSWORD` | Primer administrador; solo se usa si todavía no existe ninguno |
| `SESSION_COOKIE_SECURE` | `true` cuando la app se sirva con HTTPS |
| `SPRING_PROFILES_ACTIVE` | `development`, `test` o `production` |
| `IMAGE_TAG`, `DOCKERHUB_NAMESPACE` | Solo para `docker-compose.hub.yml` (por defecto `latest` y `jfseoanesl`) |

## Seguridad

- **Sesión y CSRF:** sesión con cookie `httpOnly` y `SameSite=Lax`, con protección CSRF de doble envío. El identificador de sesión cambia al iniciar sesión.
- **Contraseñas e intentos:** contraseñas con BCrypt; 5 intentos fallidos bloquean la cuenta 15 minutos.
- **Usuarios desactivados:** desactivar a un usuario revoca su sesión de inmediato.
- **Permisos:** se controlan en el servidor por rol, tanto por método como por URL en las rutas administrativas. La interfaz solo oculta lo que el usuario no puede usar.
- **Navegador:** cabeceras de seguridad y CSP estricta en nginx.
- **Errores y exportaciones:** los errores no exponen detalles internos, y los archivos CSV y Excel se protegen contra la inyección de fórmulas.
- **Auditoría:** se auditan tarifas, configuración, usuarios, anulaciones, cierres y correcciones de caja, mensualidades y respaldos.

Ver [`specs/10-security-specification.md`](specs/10-security-specification.md), que incluye la revisión OWASP Top 10.

## Estructura del repositorio

```text
├── backend/                 API Spring Boot (Java 21)
│   └── src/main/resources/db/
│       ├── migration/       Migraciones Flyway comunes (V1…V7)
│       ├── seed/            Datos de prueba (solo desarrollo y pruebas)
│       └── vendor/mysql/    Migraciones exclusivas de MySQL
├── frontend/                PWA React + Vite (nginx en la imagen Docker)
│   └── e2e/                 Pruebas Playwright
├── specs/                   Especificación SDD y decisiones (ADR)
├── scripts/verify-mysql.sh  Suite del backend contra un MySQL 8.4 desechable
├── docker-compose.yml       Stack de desarrollo (SQLite, puerto 8081)
├── docker-compose.prod.yml  Stack de producción (MySQL, puerto 8091)
├── docker-compose.hub.yml   Producción con imágenes de Docker Hub
├── docker-compose.small.yml Ajustes para VMs pequeñas (~1 GB de RAM)
└── .github/workflows/       CI/CD
```

## Documentación

| Documento | Contenido |
|---|---|
| [`00-product-vision.md`](specs/00-product-vision.md) | Visión, alcance y bitácora de fases y cambios |
| [`01-functional-requirements.md`](specs/01-functional-requirements.md) | Requisitos funcionales (RF-001 a RF-078) |
| [`02-non-functional-requirements.md`](specs/02-non-functional-requirements.md) | Requisitos no funcionales |
| [`03-domain-model.md`](specs/03-domain-model.md) · [`07-data-model.md`](specs/07-data-model.md) | Modelo de dominio y de datos |
| [`04-use-cases.md`](specs/04-use-cases.md) · [`05-business-rules.md`](specs/05-business-rules.md) | Casos de uso y reglas de negocio |
| [`06-architecture.md`](specs/06-architecture.md) | Arquitectura |
| [`08-api-contract.md`](specs/08-api-contract.md) · [`openapi.yaml`](specs/openapi.yaml) | Contrato de la API (OpenAPI 3) |
| [`09-ux-specification.md`](specs/09-ux-specification.md) | Navegación, pantallas y design system |
| [`10-security-specification.md`](specs/10-security-specification.md) | Seguridad y revisión OWASP |
| [`12-traceability-matrix.md`](specs/12-traceability-matrix.md) | Trazabilidad entre requisitos, reglas, código y pruebas |

**Decisiones de arquitectura** ([`specs/decisions/`](specs/decisions/)):

| ADR | Decisión |
|---|---|
| [001](specs/decisions/ADR-001-stack-tecnologico.md) | Stack tecnológico |
| [002](specs/decisions/ADR-002-persistencia-sqlite.md) | Persistencia en SQLite |
| [003](specs/decisions/ADR-003-motor-tarifario.md) | Motor de tarifas |
| [004](specs/decisions/ADR-004-autenticacion.md) | Autenticación |
| [005](specs/decisions/ADR-005-notas-modularizacion-spring-boot-4.md) | Notas de Spring Boot 4 |
| [006](specs/decisions/ADR-006-migracion-mysql.md) | Migración a MySQL |
| [007](specs/decisions/ADR-007-arranque-produccion.md) | Arranque de producción |
| [008](specs/decisions/ADR-008-exportacion-reportes.md) | Exportación de reportes |
| [009](specs/decisions/ADR-009-respaldo-base-de-datos.md) | Respaldo de la base de datos |
| [010](specs/decisions/ADR-010-mensualidades.md) | Mensualidades |
| [011](specs/decisions/ADR-011-ci-cd.md) | CI/CD con GitHub Actions y Docker Hub |
