# 10 — Especificación de Seguridad

> Fase 2 — SDD. Formaliza `prompt.md` §22 y RNF-009 a RNF-013. La decisión de mecanismo de autenticación se documenta en `decisions/ADR-004-autenticacion.md`.

## Autenticación

- Mecanismo: **sesión basada en cookie `httpOnly`, `Secure`, `SameSite=Lax`**, emitida por Spring Security tras `POST /api/auth/login`. Se prefiere sobre JWT en `localStorage` porque un token accesible desde JavaScript es robable vía XSS; una cookie `httpOnly` no lo es, y el volumen de usuarios concurrentes de un parqueadero pequeño no justifica la complejidad de un esquema stateless con refresh tokens (ver ADR-004).
- Duración de sesión: configurable, valor semilla 12 horas (cubre un turno largo). Expira por inactividad configurable (semilla: 2 horas).
- `POST /api/auth/logout` invalida la sesión en el servidor (no solo en el cliente).
- Los intentos fallidos de login no deben distinguir "usuario no existe" de "contraseña incorrecta" en el mensaje (RF-002).
- Rate limiting básico sobre `/api/auth/login` (ej. bloqueo temporal tras N intentos fallidos consecutivos por usuario/IP) para mitigar fuerza bruta — implementación mínima en el MVP (contador en memoria o tabla simple), sin requerir infraestructura adicional (Redis, etc.).

## Autorización (RBAC)

- Cada endpoint protegido declara el/los rol(es) permitido(s) mediante `@PreAuthorize("hasRole('ADMIN')")` (o equivalente) a nivel de método de servicio o controller — nunca solo a nivel de UI (BR-003, RNF-010).
- La UI oculta opciones no permitidas por rol **como ayuda de UX**, no como control de seguridad; el servidor es la única autoridad.
- Matriz de permisos base:

| Acción | Administrador | Empleado |
|---|---|---|
| Login / logout | ✅ | ✅ |
| Registrar ingreso/salida/cobro | ✅ (si opera turno) | ✅ |
| Consultar vehículos activos | ✅ | ✅ |
| Abrir/cerrar caja propia | ✅ (si opera turno) | ✅ |
| Consultar historial propio | ✅ | ✅ |
| Consultar historial global | ✅ | ❌ |
| Gestionar usuarios | ✅ | ❌ |
| Gestionar tarifas | ✅ | ❌ |
| Configurar parqueadero | ✅ | ❌ |
| Ver reportes | ✅ | ❌ |
| Ver auditoría | ✅ | ❌ |
| Anular sesión de parqueo | ✅ | ❌ |
| Corregir cierre de caja finalizado | ✅ | ❌ |
| Exportar/importar respaldo completo (RF-073) | ✅ | ❌ |

## Primer administrador de una instalación nueva

No existen credenciales por defecto en producción (`prompt.md` §33). El primer administrador se crea al arrancar a partir de `PARKING_BOOTSTRAP_ADMIN_USERNAME` y `PARKING_BOOTSTRAP_ADMIN_PASSWORD` (mínimo 8 caracteres; opcional `PARKING_BOOTSTRAP_ADMIN_FULL_NAME`), solo si todavía no existe ningún administrador. La contraseña se guarda con BCrypt y nunca se escribe en el log; la creación queda auditada. Tras el primer arranque hay que retirar la variable de contraseña del entorno y cambiarla desde la aplicación. Detalle y alternativas descartadas en `decisions/ADR-007-arranque-produccion.md`.

## Respaldo de la base de datos (RF-073, ADR-009)

- `/api/admin/**` exige rol ADMIN **por URL** (`SecurityConfig`) además de `@PreAuthorize`: un no administrador recibe 403 antes de que el servidor lea o valide el cuerpo.
- El archivo exportado contiene datos sensibles: usuarios (solo **hash BCrypt** de la contraseña, nunca texto plano), cobros y auditoría. Va sin cifrar por decisión del usuario; la pantalla advierte que debe guardarse en un lugar seguro.
- La importación reemplaza todo en una transacción, rechaza respaldos sin un administrador activo (evita dejar la instalación sin acceso) o con contraseñas que no sean BCrypt, exige escribir `REEMPLAZAR` en la UI y se audita (`DATABASE_EXPORTED` / `DATABASE_IMPORTED`, o WARN en el log si el usuario que importa no existe en los datos restaurados).
- Las sesiones de usuarios que ya no existen tras importar quedan inválidas (`ActiveUserSessionFilter`).
- Límite de 50 MB por petición en el proxy (`client_max_body_size`).

## Contraseñas

- Hash con **BCrypt** (factor de trabajo configurable, semilla 10-12), nunca reversible, nunca en texto plano (BR-004, RNF-009).
- Política mínima de contraseña (longitud mínima 8, sin lista negra compleja para no fricционar la operación de un negocio pequeño) — configurable como constante de aplicación, no hardcodeada en múltiples lugares.

## Validaciones

- **Cliente:** validación de formato (placa, campos requeridos) para UX inmediata — nunca la única barrera.
- **Servidor:** validación de autoridad vía Bean Validation (`jakarta.validation`) en los DTOs de entrada de cada endpoint, más validaciones de dominio (ej. BR-001, BR-021) ejecutadas en la capa de aplicación/dominio, no en el controller.

## Manejo centralizado de excepciones

- Un único `@ControllerAdvice` traduce excepciones de dominio/aplicación a respuestas HTTP consistentes (`400` validación, `401` no autenticado, `403` no autorizado, `404` no encontrado, `409` conflicto — ej. placa duplicada activa, `500` error interno).
- Los errores del cliente propios del framework se responden con su código correcto y un mensaje propio, nunca con el mensaje interno de Spring: ruta inexistente → `404 NOT_FOUND`; parámetro obligatorio faltante → `400 MISSING_PARAMETER`; parámetro con tipo inválido → `400 INVALID_PARAMETER`; cuerpo JSON mal formado → `400 MALFORMED_REQUEST`; método no soportado → `405 METHOD_NOT_ALLOWED`; tipo de contenido no soportado → `415`. Solo lo verdaderamente inesperado responde `500` y se registra como "Error no controlado" (corregido el 2026-09-29: antes todos estos casos caían en el manejador genérico y respondían 500).
- El cuerpo del error nunca expone stack traces, rutas de archivo ni detalles de infraestructura (RNF-012). Formato uniforme, por ejemplo:

```json
{
  "error": "CONFLICT",
  "message": "Ya existe un parqueo activo para la placa ABC123",
  "timestamp": "2026-09-22T10:15:00-05:00"
}
```

## Logging seguro

- Se registran: intentos de login (éxito/fallo, sin contraseña), errores de aplicación, operaciones críticas (ingreso, salida, cobro, cambio de tarifa, cierre de caja), con `userId`, acción y timestamp.
- Nunca se registran: contraseñas, hashes de contraseña, contenido de la cookie/token de sesión, secretos de configuración (RNF-013, prompt.md §35).

## Transporte y cabeceras

- HTTPS obligatorio en producción (terminación TLS en el proxy/servidor de despliegue — fuera del alcance de la app, documentado en `11-...`/Fase 11 Docker).
- Cabeceras de seguridad recomendadas: `Content-Security-Policy` acorde a los orígenes de la PWA, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` (o `frame-ancestors 'none'` en CSP), cookie de sesión con `Secure` habilitado solo cuando HTTPS está activo (en `development` sobre HTTP local se permite sin `Secure` mediante perfil).

## Concurrencia (referencia cruzada)

Los controles de concurrencia e idempotencia (BR-001, BR-021, BR-022 — RNF-018, RNF-019) se implementan a nivel de transacción de base de datos (restricciones únicas condicionales, ver `07-data-model.md`) como barrera definitiva, y a nivel de aplicación (validación previa) como respuesta rápida y mensaje claro al usuario.

## Revisión OWASP Top 10 (2021) — 2026-09-29

Revisión de código y configuración más pruebas activas contra la app en ejecución (local). Hallazgos y correcciones, todos verificados de nuevo en vivo tras corregir y cubiertos por pruebas (`SecurityHardeningIntegrationTest`, `LoginAttemptServiceTest`, `XlsxReportExporterTest`, `AdminIntegrationTest`):

| # | OWASP | Hallazgo | Corrección |
|---|---|---|---|
| 1 | A01/A07 | Un usuario desactivado conservaba su sesión y podía seguir operando. | `ActiveUserSessionFilter`: en cada petición verifica que el usuario exista y esté activo; si no, invalida la sesión (401). |
| 2 | A07 | Session fixation: el ID de sesión no cambiaba al iniciar sesión. | `AuthService.login` rota el ID (`changeSessionId`). |
| 3 | A06 | Tomcat 11.0.24 con 3 CVE críticos. | 11.0.25 (`tomcat.version` en el pom). |
| 4 | A07/A04 | Login sin límite de intentos. | `LoginAttemptService`: 5 fallos en 15 min bloquean ese usuario 15 min (429 `TOO_MANY_ATTEMPTS` + `Retry-After`). Por usuario, no por IP (detrás de nginx todas las peticiones tienen la misma IP); en memoria, una sola instancia. |
| 5 | A05 | Páginas sin cabeceras de seguridad; nginx mostraba su versión. | `frontend/nginx-security-headers.conf`: CSP estricta (`script-src 'self'`, sin `unsafe-inline`), `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy`; `server_tokens off`. Verificado: 0 violaciones de CSP en 9 pantallas. |
| 6 | A06 | Jackson 3.1.5 con avisos (1 HIGH de DoS). | 3.1.6 (`jackson-bom.version`). |
| 7 | A07 | Cambiar la propia contraseña no pedía la actual. | `PUT /users/me/password` exige `currentPassword` (409 `INVALID_CURRENT_PASSWORD`). |
| 8 | A05/A02 | Cookies sin `SameSite`/`Secure`. | `SameSite=Lax` en `JSESSIONID` y `XSRF-TOKEN`; `Secure` con `SESSION_COOKIE_SECURE=true` (activar al desplegar con HTTPS). |
| 9 | A09 | Intentos fallidos de login sin registro. | Se registran fallos y bloqueos (usuario, IP vía proxy, nunca la contraseña; nombres saneados contra inyección en el log). |
| 10 | A01 | Un empleado podía leer el resumen de la caja de otro. | Solo el dueño o un administrador (403). |
| 11 | A06 | vitest/vite/esbuild vulnerables (herramientas de desarrollo). | vitest 4.1.11: `npm audit` sin vulnerabilidades. |
| 12 | A02 | La app se sirve solo por HTTP. | **Pendiente de despliegue**: terminar TLS (HTTPS) en producción y agregar HSTS; con eso activar `SESSION_COOKIE_SECURE`. |

Sin hallazgos (verificado): inyección SQL, XSS almacenado, inyección de fórmulas en el Excel exportado, enumeración de usuarios (mensaje y tiempo iguales), CSRF, CORS, Actuator (solo `health`), BCrypt, errores sin detalles internos, SSRF (el backend no hace peticiones salientes), secretos en el repositorio, dependencias del frontend que llegan al navegador.

**Límite conocido:** cambiar la contraseña de un usuario no cierra sus otras sesiones abiertas (sí lo hace desactivarlo).

