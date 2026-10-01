# 02 — Requisitos No Funcionales

> Fase 1 — SDD. Formaliza y amplía `00-product-vision.md` §7. Cada requisito incluye un criterio verificable cuando es posible, para que pueda comprobarse en `11-test-strategy.md` y en QA (Fase 10).

## Rendimiento y usabilidad operativa

| ID | Descripción | Criterio de verificación |
|---|---|---|
| RNF-001 | El flujo de registro de ingreso (placa + tipo + confirmar) debe completarse en un máximo de 3 interacciones táctiles/click tras abrir la pantalla de "Nuevo ingreso". | Revisión de UX flow en `09-ux-specification.md`; prueba E2E cronometrando pasos. |
| RNF-002 | La búsqueda de placa en "Vehículos parqueados" debe filtrar resultados de forma incremental mientras se escribe (sin necesidad de confirmar búsqueda). | Prueba E2E / componente. |
| RNF-003 | Las pantallas operativas (ingreso, parqueados, salida/cobro, caja) deben cargar su contenido principal en menos de 2 segundos en condiciones normales de red sobre el entorno de desarrollo. | Medición manual / prueba de rendimiento básica. |
| RNF-004 | Los formularios de operación diaria deben tener el mínimo de campos obligatorios (placa y tipo de vehículo para ingreso; medio de pago para salida). | Revisión de diseño de formularios. |

## PWA y disponibilidad

| ID | Descripción | Criterio de verificación |
|---|---|---|
| RNF-005 | La aplicación debe ser instalable como PWA (manifest válido, iconos en los tamaños requeridos, service worker registrado). | Auditoría Lighthouse PWA / instalación manual en Android, desktop e iOS (dentro de sus límites). |
| RNF-006 | La interfaz debe ser responsive y usable en computador, tablet y smartphone, sin scroll horizontal ni elementos cortados. | Pruebas manuales en breakpoints definidos en `09-ux-specification.md`. |
| RNF-007 | El shell de la aplicación (assets estáticos) debe seguir siendo accesible ante pérdida temporal de conectividad, mostrando un aviso claro de falta de conexión para operaciones transaccionales. | Prueba manual: activar modo avión y verificar comportamiento (BR-019). |
| RNF-008 | La aplicación debe operar en modo standalone al instalarse (sin barra de navegador). | Verificación manual post-instalación. |

## Seguridad

| ID | Descripción | Criterio de verificación |
|---|---|---|
| RNF-009 | Las contraseñas se almacenan con algoritmo de hash con salt (ej. BCrypt), nunca en texto plano. | Revisión de código / `10-security-specification.md`. |
| RNF-010 | Toda ruta/endpoint protegido debe validar autenticación y autorización en el servidor, independientemente de lo que oculte el cliente. | Pruebas de integración intentando acceder sin token / con rol incorrecto. |
| RNF-011 | Las entradas de usuario deben validarse tanto en cliente (UX) como en servidor (autoridad). | Pruebas de integración con payloads inválidos. |
| RNF-012 | El manejo de excepciones debe ser centralizado y no debe exponer detalles internos (stack traces, rutas de archivo) al cliente. | Revisión de código / prueba de error simulado. |
| RNF-013 | Los logs no deben contener contraseñas, tokens ni secretos. | Revisión de código y de logs generados en pruebas. |

## Persistencia y portabilidad

| ID | Descripción | Criterio de verificación |
|---|---|---|
| RNF-014 | La persistencia por defecto en desarrollo debe usar SQLite en archivo (`parking.db`), no solo en memoria, salvo perfil de pruebas automatizadas explícito. | Revisión de configuración por perfil (`development`, `test`, `production`). |
| RNF-015 | La capa de dominio no debe contener SQL específico de SQLite ni tipos exclusivos de ese motor. | Revisión de código: dominio libre de referencias JDBC/SQL directo. |
| RNF-016 | El cambio de SQLite a MySQL debe requerir solo configuración de datasource, driver y migraciones compatibles, sin modificar servicios de dominio. | Verificación práctica en Fase 12 (cambio de perfil y ejecución de suite de pruebas). |
| RNF-017 | El esquema de base de datos debe versionarse mediante migraciones (Flyway), evitando cambios manuales de esquema. | Revisión de carpeta de migraciones. |

## Concurrencia y consistencia

| ID | Descripción | Criterio de verificación |
|---|---|---|
| RNF-018 | El backend debe impedir mediante control transaccional que dos solicitudes concurrentes generen dos sesiones activas para la misma placa. | Prueba de integración con solicitudes concurrentes simuladas. |
| RNF-019 | El backend debe impedir doble cobro sobre la misma sesión ante doble envío (doble clic, reintento de red). | Prueba de integración con solicitudes duplicadas (BR-021). |

## Observabilidad

| ID | Descripción | Criterio de verificación |
|---|---|---|
| RNF-020 | El sistema debe registrar logs de: inicio de sesión, errores, operaciones críticas, cálculo de tarifas, cierre de caja. | Revisión de logs generados durante pruebas E2E. |
| RNF-021 | El backend debe exponer un endpoint de health check apto para monitoreo básico. | Prueba de integración `GET /actuator/health` (o equivalente). |

## Mantenibilidad y extensibilidad

| ID | Descripción | Criterio de verificación |
|---|---|---|
| RNF-022 | La arquitectura debe permitir agregar un nuevo tipo de vehículo sin modificar el motor de cálculo existente. | Revisión de diseño (Strategy/Factory) en `06-architecture.md`. |
| RNF-023 | La arquitectura debe permitir agregar una nueva modalidad de tarifa (ej. nocturna) implementando una nueva estrategia, sin alterar las existentes. | Revisión de diseño; prueba unitaria añadiendo una estrategia de ejemplo. |
| RNF-024 | El código entregado debe compilar, incluir todas las dependencias necesarias y no dejar TODOs críticos, métodos vacíos ni endpoints simulados. | Revisión de PR / build CI. |

## Portabilidad de zona horaria y localización

| ID | Descripción | Criterio de verificación |
|---|---|---|
| RNF-025 | Toda la lógica de fecha/hora debe pasar por un único servicio de tiempo (no dispersa en controladores o servicios individuales). | Revisión de código: única fuente de `now()`. |
| RNF-026 | La zona horaria y la moneda deben ser configurables por parqueadero, con valores semilla `America/Bogota` y `COP`. | Revisión de `Configuration` y pruebas de cambio de configuración. |
