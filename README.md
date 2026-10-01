# Parqueadero — PWA de gestión de parqueadero (Fase 12: Preparación MySQL)

Ver `/specs` para la especificación completa (SDD). Este README cubre solo cómo ejecutar lo entregado hasta ahora: autenticación, estructura del backend/frontend y PWA base.

## Backend (Java 21 + Spring Boot 4.1.1)

```bash
cd backend
mvn spring-boot:run
```

- Perfil por defecto: `development` (SQLite en archivo `backend/data/parking.db`, se crea solo).
- Migraciones (Flyway) y datos semilla se aplican automáticamente al arrancar.
- Health check: `GET http://localhost:8080/actuator/health`
- Pruebas: `mvn test` (usa SQLite en memoria, perfil `test`).

Usuarios de prueba (**solo desarrollo, nunca usar en producción** — prompt.md §33):

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | Administrador |
| `empleado` | `empleado123` | Empleado |

## Frontend (React + TypeScript + Vite + Tailwind, PWA)

```bash
cd frontend
npm install
npm run dev
```

- Abre `http://localhost:5173`. El servidor de desarrollo reenvía `/api/**` al backend en `http://localhost:8080` (mismo origen, sin problemas de CORS/cookies).
- `npm run build` genera el bundle de producción e incluye el service worker (Workbox).
- Pruebas unitarias/componentes: `npm run test` (Vitest + Testing Library).
- Pruebas E2E/responsive (requieren backend y frontend corriendo, y `npx playwright install chromium` una vez): `npm run test:e2e` (Playwright). Inician sesión como `admin/admin123`. Contra el stack de producción (MySQL, sin datos de prueba), primero crear al menos una tarifa para automóviles con el administrador inicial y luego:
  ```bash
  E2E_BASE_URL=http://localhost:8091 E2E_ADMIN_USERNAME=<admin inicial> E2E_ADMIN_PASSWORD=<clave> npm run test:e2e
  ```
  `E2E_BROWSER_CHANNEL=chrome` usa el Chrome instalado en lugar del Chromium de Playwright.

## Qué incluye esta entrega

**Fase 4 — Foundation:**
- Autenticación por sesión (cookie `httpOnly` + CSRF) — UC-001, `decisions/ADR-004-autenticacion.md`.
- RBAC base (roles ADMIN/EMPLOYEE) con `@PreAuthorize`.
- Persistencia SQLite con Flyway (`backend/src/main/resources/db/migration`), esquema completo de `07-data-model.md`, preparado para migrar a MySQL sin tocar el dominio.
- Manejo centralizado de errores y logging (sin datos sensibles).
- PWA instalable (manifest + service worker) con estrategia cache-first solo para el shell estático.
- Estructura de navegación y design system (Tailwind) de `09-ux-specification.md`.

**Fase 5 — Operación principal:**
- Nuevo ingreso, vehículos parqueados (búsqueda), salida y cobro atómico, comprobante de pago.
- Motor de tarifas (Strategy + Factory + resolución por prioridad) con 25 pruebas de borde: tolerancia, fracciones/redondeo, franjas AM/PM (incl. cruce de medianoche), "todo el día", empates de prioridad y permanencias mayores a 24h.
- Apertura de caja (dependencia técnica mínima para poder cobrar).
- Ver `05-business-rules.md` y `decisions/ADR-005-notas-modularizacion-spring-boot-4.md` para las reglas de cálculo precisas y hallazgos técnicos del stack.

**Fase 6 — Caja:**
- Cierre de caja con arqueo (efectivo esperado vs. reportado, diferencia) — solo el efectivo cuenta como físico, ver BR-023.1.
- Corrección administrativa de un cierre ya finalizado, con motivo obligatorio y registro de auditoría (RF-057).
- Pantalla de Caja en el frontend (abrir / resumen en vivo / cerrar).

**Fase 7 — Administración:**
- Usuarios: alta, activar/desactivar, cambio de contraseña (propio y por administrador).
- Tarifas: alta/edición/activación con validación por modalidad (RF-021) y auditoría.
- Configuración del parqueadero + catálogos de tipos de vehículo y medios de pago.
- Auditoría: consulta filtrable por entidad; anulación administrativa de una sesión de parqueo (UC-011).
- Pantallas de Usuarios, Tarifas, Configuración y Auditoría en el frontend.

**Fase 8 — Reportes:**
- Reportes diario/semanal/mensual/rango personalizado: vehículos ingresados/finalizados/anulados, recaudo total y por tipo de vehículo/medio de pago/tarifa/empleado, duración promedio, hora de mayor movimiento, diferencias de caja del período.
- Dashboard del Administrador (reemplaza el Home placeholder): ocupación vs. capacidad, ingresos/salidas/recaudo de hoy, cajas abiertas, últimos movimientos — se refresca cada 30s.
- Verificado end-to-end en navegador real: apertura de caja → ingreso → cobro → cierre de caja, con Dashboard y Reportes reflejando la actividad en cada paso.

**Fase 9 — PWA y UX (validación):**
- Manifest e instalación verificados en el navegador (iconos 192/512/maskable, `display: standalone`, service worker activo controlando la página).
- Cache del shell estático confirmada (Workbox precache, recargas servidas sin red); `/api/**` sigue sin cachear (BR-019).
- Corrección: aviso claro de "sin conexión" ante fallos de red (antes cada pantalla mostraba su error genérico) — `frontend/src/api/client.ts`.
- Corrección: `<html lang="es">` (antes `en`) para una app enteramente en español.
- Responsive validado por revisión de código (sin anchos fijos ni tablas, contenedores fluidos, `Sidebar`/`BottomNav` mobile-first); verificado después en viewport móvil real por las pruebas E2E del proyecto `mobile` (`e2e/responsive.spec.ts`, incluye Historial y Auditoría).

**Fase 10 — QA:**
- Backend: 49 pruebas (43 previas + `SecurityIntegrationTest`, nueva — RBAC, CSRF, hash de contraseñas, no-fuga de errores internos), todas en verde.
- Frontend: primer tooling de pruebas del proyecto (Vitest + Testing Library) — 16 pruebas unitarias/componentes, todas en verde.
- E2E y responsive: suite de Playwright escrita y type-checada (`frontend/e2e/`), **no ejecutada** en la sesión de desarrollo por una restricción de red del entorno (bloqueo al descargar el binario de Chromium) — ver detalle en `specs/00-product-vision.md` Fase 10. Lista para correr con `npm run test:e2e` en cuanto haya acceso de red (o en CI).

**Revisión de seguridad OWASP (2026-09-29):** límite de intentos de login (5 fallos → 15 min de bloqueo), rotación de sesión al iniciar sesión, revocación inmediata de la sesión al desactivar un usuario, cambio de la propia contraseña con la actual, cabeceras de seguridad (CSP estricta) en nginx, cookies `SameSite=Lax`, registro de intentos fallidos y dependencias parcheadas. Al desplegar con HTTPS, definir `SESSION_COOKIE_SECURE=true`. Ver `specs/10-security-specification.md`.

**Mejoras posteriores a la Fase 12 (2026-09-29):**
- **Exportación de reportes a Excel y PDF** (RF-063): botones "Excel" y "PDF" en Reportes; exportan el período que se está viendo, con las mismas cifras que la pantalla. Se generan en el servidor (Apache POI y OpenPDF). Ver `specs/decisions/ADR-008-exportacion-reportes.md`.
- **Auditoría de tarifas completa:** al editar una tarifa se guarda el valor anterior y el nuevo de todos los campos que afectan el cálculo, incluido el horario.
- La sección "Tarifa" del cobro se muestra siempre (con una sola tarifa aplicable, fija y con la razón).

**Rediseño UI/UX (previo a la Fase 12):**
- Identidad visual nueva (ver `specs/09-ux-specification.md` §3.4): placa estilo colombiano como elemento principal, íconos SVG (Lucide) en lugar de emojis, tipografía Fira empaquetada localmente, barra lateral oscura y barra inferior móvil con íconos.
- Componentes reutilizables de §4 en `frontend/src/components/`, incluidos `ConfirmDialog` (cobro y cierre de caja) y `Toast` (ingreso, tarifas, usuarios, configuración).
- Mejoras de flujo: accesos rápidos en el inicio del empleado; normalización de placa en vivo (BR-002); ingreso listo para el siguiente vehículo sin navegar; diferencia de arqueo calculada mientras se escribe el conteo; tabla en escritorio y tarjetas en móvil para Parqueados; desgloses de reportes en barras; detalle de valor anterior/nuevo en Auditoría.
- Correcciones encontradas al verificar: el proxy de nginx rechazaba el login desde el navegador (403 CORS, `frontend/nginx.conf`); SQLite devolvía 500 con escrituras concurrentes (`application-development.yml`: `transaction_mode=IMMEDIATE`, `busy_timeout`, WAL); una mutación enviada antes de existir la cookie CSRF salía sin token (`frontend/src/api/client.ts`).
- Verificado: 17 pruebas unitarias del frontend, 49 del backend (el total de "53" reportado originalmente estaba inflado por reportes de Surefire viejos; hoy son 57, ver Fase 12) y la suite E2E de Playwright (12/12) contra el stack de desarrollo y el de Docker. BR-008.1 (tarifa alterna al cobrar) y la validación del valor cobrado se agregaron después, ver `specs/05-business-rules.md`.

## Producción (MySQL)

Copia `.env.example` a `.env`, complétalo, y arranca con `SPRING_PROFILES_ACTIVE=production`. Procedimiento completo y verificado (Fase 12): `specs/decisions/ADR-006-migracion-mysql.md`. Ver también "Despliegue con Docker" abajo para el camino contenedorizado equivalente.

## Despliegue con Docker

Requiere Docker y Docker Compose. Hay dos stacks, uno para cada perfil del backend. Usan puertos y nombres de proyecto distintos (`parking` y `parking-prod`), así que pueden correr a la vez.

**Desarrollo/demo (SQLite, sin configuración adicional):**

```bash
docker compose up --build
```

- Frontend (con la PWA servida por nginx, proxy interno de `/api/**` al backend): http://localhost:8081
- Backend, acceso directo opcional (health check, depuración): http://localhost:18080/actuator/health
- Los datos de SQLite persisten en el volumen con nombre `parking_backend_data` entre reinicios (`docker compose down` sin `-v`).

**Producción (MySQL):**

```bash
cp .env.example .env   # editar DB_USER / DB_PASSWORD / CORS_ALLOWED_ORIGINS / PARKING_BOOTSTRAP_ADMIN_*
docker compose -f docker-compose.prod.yml up --build -d
```

- Frontend: http://localhost:8091
- Backend: http://localhost:18090/actuator/health
- MySQL, acceso directo opcional (ej. para inspección con un cliente): `localhost:13306`
- Sin datos semilla ni credenciales por defecto (prompt.md §33). Los datos de referencia (roles, tipos de vehículo, medios de pago, configuración inicial) se crean con la migración V4. **El primer administrador** se crea al arrancar: define `PARKING_BOOTSTRAP_ADMIN_USERNAME` y `PARKING_BOOTSTRAP_ADMIN_PASSWORD` (mínimo 8 caracteres) en `.env` antes del primer `up`; después retira la contraseña del `.env` y cámbiala desde la aplicación. Luego crea las tarifas y completa la configuración. Ver `specs/decisions/ADR-007-arranque-produccion.md`.
- Procedimiento completo del cambio SQLite → MySQL y comprobaciones posteriores: `specs/decisions/ADR-006-migracion-mysql.md` (Fase 12).

**Fase 12 — Preparación MySQL (verificada):**
- La aplicación funciona sobre MySQL 8.4 **sin cambios en el código Java**: la suite del backend pasa 57/57 en SQLite y en MySQL, y la suite E2E 12/12 contra el stack de producción con MySQL.
- Arranque de producción resuelto después (ADR-007): datos de referencia (V4) y primer administrador desde el entorno. Suite del backend: 62/62 en ambos motores.
- Para repetir la verificación del código contra MySQL (requiere Docker): `bash scripts/verify-mysql.sh`.
- Las diferencias de motor viven en `backend/src/main/resources/db/vendor/{motor}` (Flyway `classpath:db/vendor/{vendor}`). Hoy solo MySQL tiene una (`V3_1`: 18 llaves foráneas que MySQL ignoraba y fechas `DATETIME(6)`).

Ambos stacks se verificaron manualmente en esta sesión: build de las dos imágenes, arranque con healthchecks en verde, login a través del proxy de nginx (mismo mecanismo de doble envío CSRF que en desarrollo; esa verificación se hizo con curl, sin cabecera `Origin`, y no detectó que el navegador recibía 403 — corregido después en `frontend/nginx.conf`, ver "Rediseño UI/UX"), fallback de rutas de la SPA (`/reportes` sin recargar), PWA (`manifest.webmanifest`/`sw.js` servidos) y persistencia de datos de SQLite tras reiniciar el contenedor del backend.
