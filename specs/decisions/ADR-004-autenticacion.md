# ADR-004 — Autenticación: sesión con cookie httpOnly en lugar de JWT en almacenamiento del cliente

**Estado:** Aceptado (2026-09-22)

## Contexto

La PWA necesita autenticar Administrador y Empleado, proteger rutas y endpoints (RBAC), y funcionar razonablemente bien como app instalada. `prompt.md` §22 exige manejo seguro de sesiones o tokens, sin más restricción explícita sobre el mecanismo.

## Problema

Elegir entre un esquema stateless (JWT) o basado en sesión de servidor (cookie), considerando el tamaño y perfil de uso del sistema (parqueadero pequeño, pocos usuarios concurrentes, un backend, prioridad en simplicidad de mantenimiento sobre escalabilidad horizontal).

## Alternativas consideradas

- **(a) JWT** (access token de corta duración + refresh token), almacenado en memoria del cliente (o `localStorage`, con el riesgo de XSS que eso implica) y enviado como `Authorization: Bearer`.
- **(b) Sesión de servidor con cookie `httpOnly`, `Secure`, `SameSite`**, gestionada por Spring Security, con protección CSRF para operaciones de escritura.

## Decisión

**(b) Sesión con cookie `httpOnly`.**

## Justificación

- Superficie de ataque menor ante XSS: un token en `localStorage`/memoria accesible por JS puede ser exfiltrado por un script malicioso; una cookie `httpOnly` no es legible desde JavaScript.
- Simplicidad: no hay que implementar rotación/refresco de tokens, ni sincronizar expiración entre pestañas — Spring Security lo resuelve de forma estándar.
- Revocación inmediata: cerrar sesión invalida el estado en el servidor al instante; con JWT stateless la revocación antes de que expire el token requiere infraestructura adicional (listas de revocación), innecesaria para este tamaño de sistema.
- El sistema opera con un único backend, sin necesidad de escalar horizontalmente sin sesión compartida en el corto/mediano plazo — el argumento clásico a favor de JWT stateless (escalabilidad sin estado compartido) no aplica aquí con el peso suficiente para justificar su complejidad adicional.

## Consecuencias

- Se requiere protección CSRF (token de doble envío o el mecanismo estándar de Spring Security) en todas las operaciones de escritura, ya que las cookies se envían automáticamente por el navegador.
- El cliente (React PWA) debe usar `fetch`/`axios` con `credentials: 'include'` y manejar `401` redirigiendo a login.
- Si en el futuro se requiere un cliente externo desacoplado (app móvil nativa, integraciones de terceros) que no maneje cookies de forma natural, se revisará esta decisión con un nuevo ADR — no se anticipa esa necesidad en el alcance actual.
- Duración de sesión y expiración por inactividad quedan configurables (ver `10-security-specification.md`), con valores semilla de 12h/2h ajustados a la duración típica de un turno.
