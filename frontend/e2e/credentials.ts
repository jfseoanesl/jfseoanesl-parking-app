/**
 * Credenciales del administrador con el que corren las pruebas E2E. Por defecto, el usuario semilla
 * de desarrollo; contra un stack de produccion (sin semilla) se pasan las del administrador inicial
 * (ADR-007) con E2E_ADMIN_USERNAME / E2E_ADMIN_PASSWORD.
 */
export const ADMIN_USERNAME = process.env.E2E_ADMIN_USERNAME ?? 'admin'
export const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? 'admin123'
