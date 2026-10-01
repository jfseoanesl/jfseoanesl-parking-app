# ADR-001 — Stack tecnológico

**Estado:** Aceptado (2026-09-22)

## Contexto

Se requiere seleccionar el stack para una PWA de gestión de parqueadero pequeño, con backend en capas, persistencia inicial en SQLite migrable a MySQL, y frontend instalable como PWA. `prompt.md` §27 propone un stack de referencia a usar salvo objeción.

## Problema

Elegir tecnologías maduras y ampliamente soportadas, evitando dependencias innecesarias, y fijar una única estrategia de UI (el prompt exige no mezclar librerías de estilos).

## Alternativas consideradas

- **Backend:** Java + Spring Boot vs. Node.js + Express/NestJS vs. Python + FastAPI/Django.
- **UI:** Material UI (componentes prediseñados) vs. Tailwind CSS (utility-first) vs. Chakra UI.

## Decisión

- Backend: **Java + Spring Boot**, API REST, Spring Data JPA, Flyway para migraciones.
- Frontend: **React + TypeScript + Vite**.
- PWA: **vite-plugin-pwa / Workbox**.
- UI: **Tailwind CSS** (confirmado explícitamente por el usuario el 2026-09-22).
- Persistencia: SQLite (desarrollo) → MySQL (producción futura).
- Pruebas: JUnit + Mockito (backend), Vitest + Testing Library (frontend), Playwright (E2E).
- Contenedores: Docker + Docker Compose.

## Justificación

- Spring Boot: ecosistema maduro para RBAC (Spring Security), transacciones, JPA/Flyway multi-motor, y con la curva de aprendizaje más baja para un equipo que ya trabaja con Java en este contexto.
- React + Vite: arranque y HMR rápidos, tipado con TypeScript reduce errores en un dominio con muchas reglas de negocio (tarifas).
- Tailwind sobre Material UI: al ser utility-first no impone un "look" de librería de componentes que haya que sobrescribir para lograr los botones grandes y táctiles que exige la UX del flujo crítico (prompt.md §6, §20); genera menos CSS no usado (mejor para el peso de la PWA) y da control total sobre el diseño mobile-first sin pelear contra componentes preestilizados.
- SQLite→MySQL: ambos soportados de forma madura por Spring Data JPA/Hibernate y Flyway, sin necesidad de ORM adicional.

## Consecuencias

- Se requiere disciplina para no introducir sintaxis SQL específica de un motor en migraciones ni en repositorios (mitigado en `06-architecture.md` y `07-data-model.md`).
- Tailwind requiere definir un design system de utilidades consistente (tokens de color, espaciado) desde `09-ux-specification.md` para no perder consistencia visual al no haber componentes prediseñados.
- Sin librería de componentes, algunos componentes de UI (modales, date pickers) se construyen a medida o con librerías headless ligeras compatibles con Tailwind — decisión a detallar en Fase 3 (UX/UI) si se requiere.
