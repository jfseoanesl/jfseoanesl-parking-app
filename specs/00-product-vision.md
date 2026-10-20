# 00 — Visión del Producto y Descubrimiento (FASE 0)

> Estado: **aprobado**. El usuario confirmó las recomendaciones por defecto de la sección 15 el 2026-09-22. Esas decisiones quedaron formalizadas como reglas de negocio en `05-business-rules.md`. Este documento sigue siendo la base de las siguientes fases SDD (`01-functional-requirements.md`, `02-non-functional-requirements.md`, etc.).

---

## 1. Visión del producto

Construir una **PWA de gestión de parqueadero pequeño** cuyo criterio de éxito principal es la **velocidad operativa**: un empleado debe poder registrar un ingreso o cobrar una salida en el menor número de toques posible, sin curva de aprendizaje significativa, usando indistintamente un computador, una tablet o un teléfono.

El sistema debe ser simple de operar el día uno, pero construido sobre una arquitectura que permita evolucionar (nuevos tipos de vehículo, nuevas modalidades de tarifa, multi-tarifa, migración a MySQL, sincronización offline) sin reescribir la lógica de negocio ya validada.

El flujo crítico que todo el diseño debe proteger es:

**INGRESO → BÚSQUEDA → SALIDA → COBRO**

## 2. Alcance de la versión MVP

Incluye, con funcionalidad completa:

- Autenticación y RBAC (Administrador / Empleado).
- Registro de ingreso de vehículos (autos y motos) con normalización de placa.
- Pantalla de vehículos actualmente estacionados (búsqueda, filtro, tiempo transcurrido).
- Registro de salida con cálculo automático de tarifa y cobro obligatorio (salvo anulación administrativa auditada).
- Motor de tarifas desacoplado, con al menos: tarifa plena AM, tarifa plena PM, tarifa todo el día, tarifa por tiempo — configurables desde base de datos, no desde código.
- Registro de pagos (efectivo, transferencia, otros) y comprobante básico imprimible/digital.
- Caja: apertura, movimientos del turno, cierre con arqueo (efectivo esperado vs. reportado).
- Historial de operaciones con filtros básicos.
- Reportes: diario, semanal, mensual y por rango personalizado, con los indicadores mínimos listados en la sección 13 del prompt maestro (sin exportación PDF/Excel/CSV obligatoria en el MVP).
- Dashboard operativo (ocupación, ingresos/salidas del día, recaudo del día, estado de caja).
- Auditoría de operaciones sensibles (tarifas, anulaciones, cierres de caja, usuarios).
- Administración de usuarios, tarifas y configuración del parqueadero.
- PWA instalable, responsive, con cache de recursos estáticos para tolerar cortes breves de conectividad.
- Persistencia en SQLite con arquitectura preparada para migrar a MySQL sin tocar la lógica de dominio.

## 3. Funcionalidades fuera del alcance inicial

Explícitamente pospuestas (la arquitectura debe dejar espacio para ellas, pero no se implementan en el MVP):

- Sincronización offline transaccional (operar sin conexión y sincronizar después). Solo se garantiza offline de recursos estáticos de la PWA.
- Exportación real a PDF / Excel / CSV (se deja el diseño preparado, pero no es obligatorio implementarlas).
- Tarifas avanzadas: nocturna, fin de semana, festivos, convenios, mensualidades, clientes frecuentes, vehículos exentos.
- Tipos de vehículo adicionales a auto y moto (la arquitectura debe permitirlos, pero no se cargan por defecto).
- Migración real a MySQL en producción (se deja preparada, pero el MVP opera sobre SQLite).
- Multi-sede / multi-parqueadero en una misma instalación.
- Notificaciones push, impresión térmica directa (se diseña el comprobante para impresión estándar del navegador).
- Integraciones de pago electrónico reales (pasarelas); "transferencia" se registra como medio de pago manual, no como integración.

## 4. Actores

- **Administrador**: gestiona usuarios, tarifas, configuración, reportes, auditoría y correcciones autorizadas.
- **Empleado**: opera el día a día (ingreso, búsqueda, salida, cobro, caja de su turno).

(Detalle completo de permisos en sección 3 de `prompt.md`; se formalizará como matriz RBAC en `01-functional-requirements.md`.)

## 5. Mapa general de módulos

Agrupados por eje funcional (los 18 módulos de la sección 4 del prompt maestro):

| Eje | Módulos |
|---|---|
| Identidad y acceso | Autenticación, Usuarios y roles |
| Configuración base | Configuración del parqueadero, Tipos de vehículo, Tarifas, Configuración PWA |
| Operación diaria | Registro de ingreso, Vehículos actualmente estacionados, Registro de salida |
| Cálculo y cobro | Motor de cálculo tarifario, Gestión de cobros, Caja |
| Visibilidad y control | Historial de operaciones, Reportes, Dashboard, Auditoría |
| Plataforma | Persistencia, Respaldo y recuperación futura |

La arquitectura (capas + repository pattern, ver punto 13) debe permitir agregar módulos nuevos sin reestructurar los existentes.

## 6. Requisitos funcionales preliminares

Numeración preliminar — se formalizará y ampliará en `01-functional-requirements.md`.

| ID | Requisito |
|---|---|
| RF-001 | El sistema debe permitir a un empleado autenticado registrar el ingreso de un vehículo indicando placa y tipo de vehículo, asignando fecha/hora automáticamente. |
| RF-002 | El sistema debe normalizar la placa (mayúsculas, sin espacios extra, formato validado) antes de guardarla. |
| RF-003 | El sistema no debe permitir dos parqueos activos simultáneos con la misma placa. |
| RF-004 | El sistema debe listar los vehículos actualmente estacionados con placa, tipo, hora de ingreso, tiempo transcurrido y empleado que registró el ingreso. |
| RF-005 | El sistema debe permitir buscar un vehículo activo por placa y registrar su salida. |
| RF-006 | El sistema debe calcular automáticamente el valor a pagar al momento de la salida, según las reglas de tarifa vigentes. |
| RF-007 | El sistema no debe permitir finalizar un parqueo sin registrar el cobro, salvo anulación administrativa expresa y auditada. |
| RF-008 | El administrador debe poder crear, editar y activar/desactivar tarifas sin necesidad de cambios de código. |
| RF-009 | El sistema debe registrar cada pago con valor calculado, valor cobrado, medio de pago, fecha, hora, usuario y número de operación. |
| RF-010 | El sistema debe permitir apertura y cierre de caja por turno, con arqueo (efectivo esperado vs. reportado). |
| RF-011 | El sistema debe registrar auditoría de cambios de tarifa, anulaciones, cierres de caja y modificaciones de usuario. |
| RF-012 | El sistema debe generar reportes diario, semanal, mensual y por rango personalizado con los indicadores mínimos definidos en el prompt maestro. |
| RF-013 | El sistema debe restringir funcionalidades administrativas al rol Administrador (RBAC). |
| RF-014 | El sistema debe permitir emitir un comprobante de pago imprimible/digital con los datos mínimos definidos (placa, tiempo, tarifa, valor, medio de pago, consecutivo, empleado). |

## 7. Requisitos no funcionales preliminares

| ID | Requisito |
|---|---|
| RNF-001 | El flujo ingreso→salida→cobro debe minimizar el número de interacciones (objetivo cualitativo: pocos toques, sin pasos redundantes). |
| RNF-002 | La aplicación debe ser instalable como PWA (manifest, service worker, iconos) y funcionar en modo standalone. |
| RNF-003 | La interfaz debe ser responsive y usable en computador, tablet y smartphone (Android e iOS dentro de las limitaciones de PWA en iOS). |
| RNF-004 | Las contraseñas deben almacenarse hasheadas; nunca en texto plano. |
| RNF-005 | El acceso a funcionalidades debe estar controlado por RBAC, validado tanto en cliente como en servidor. |
| RNF-006 | La persistencia debe iniciar sobre SQLite en archivo (no solo en memoria) y estar preparada para migrar a MySQL sin reescribir lógica de dominio. |
| RNF-007 | Los recursos estáticos de la PWA deben quedar disponibles ante pérdida temporal de conectividad (offline de shell, no offline transaccional). |
| RNF-008 | Las operaciones críticas (login, errores, cálculo de tarifa, cierre de caja) deben quedar registradas en logs, sin exponer contraseñas, tokens ni secretos. |
| RNF-009 | El sistema debe evitar condiciones de carrera conocidas: doble cobro, doble salida, doble ingreso activo para la misma placa, doble envío por doble clic. |
| RNF-010 | El backend debe exponer un health check. |

## 8. Reglas de negocio que requieren definición

Estas reglas no pueden implementarse sin antes cerrar una decisión (ver tabla de decisiones pendientes, sección 15):

- Condiciones horarias exactas que activan tarifa plena AM vs. PM.
- Condición que activa la tarifa "todo el día".
- Manejo de fracciones de tiempo y redondeo en la tarifa por tiempo.
- Tolerancia (minutos de gracia sin cobro) y si aplica solo al ingreso, a la salida, o a ambos.
- Prioridad entre reglas tarifarias cuando varias coinciden para el mismo tipo de vehículo y horario.
- Qué ocurre cuando el parqueo cruza el límite AM/PM.
- Qué ocurre cuando el parqueo cruza medianoche.
- Qué ocurre con permanencias superiores a 24 horas.
- Si existen tarifas distintas por tipo de vehículo (auto vs. moto) desde el MVP o se definen iguales inicialmente.

## 9. Riesgos técnicos

- **Motor tarifario mal desacoplado**: si las reglas quedan embebidas en el código en lugar de ser datos configurables, cualquier ajuste de tarifa requerirá despliegue — se mitiga con el diseño Strategy/Factory/Chain of Responsibility exigido en el prompt.
- **Migración SQLite → MySQL**: riesgo de introducir SQL o tipos específicos de SQLite en la capa de dominio — se mitiga con Repository Pattern, JPA y Flyway desde el inicio.
- **Limitaciones de PWA en iOS Safari** (notificaciones, almacenamiento, instalación): puede limitar la experiencia offline y de instalación en ese entorno.
- **Concurrencia**: dos empleados operando sobre la misma placa o la misma caja simultáneamente; requiere control transaccional e idempotencia en los endpoints críticos.
- **Zona horaria / manejo de fechas disperso**: si la lógica de fecha/hora no se centraliza en un servicio único, aparecerán inconsistencias entre cálculo de tarifa, reportes y auditoría.

## 10. Riesgos funcionales

- Ambigüedad en las reglas AM/PM y cruce de medianoche puede generar cobros incorrectos y reclamos de clientes.
- Falta de bloqueo efectivo de "placa duplicada activa" puede generar dos sesiones abiertas para el mismo vehículo.
- Permitir cerrar un parqueo sin cobro (sin control administrativo) puede causar fugas de caja.
- Cierres de caja con diferencias no quedan auditados si no se fuerza registro y trazabilidad.
- Empleados con acceso a funciones administrativas por defectos de RBAC mal implementado.

## 11. Glosario inicial

| Término | Definición |
|---|---|
| Parqueo activo / sesión de parqueo | Registro de un vehículo que ingresó y aún no ha registrado salida (estado `EN_PARQUEADERO`). |
| Tarifa plena AM | Tarifa fija aplicable a permanencias dentro de la franja horaria de la mañana definida por el administrador. |
| Tarifa plena PM | Tarifa fija aplicable a permanencias dentro de la franja horaria de la tarde/noche definida por el administrador. |
| Tarifa todo el día | Valor fijo aplicable cuando la permanencia cumple la regla configurada de "día completo". |
| Tarifa por tiempo | Cobro proporcional al tiempo de permanencia, según unidad, fracción y redondeo configurados. |
| Fracción | Unidad mínima de tiempo que se cobra como bloque completo aunque no se haya cumplido en su totalidad. |
| Tolerancia | Minutos de gracia durante los cuales no se cobra tiempo adicional. |
| Cierre de caja / arqueo | Proceso de fin de turno que compara el efectivo esperado (según transacciones) contra el efectivo reportado por el empleado. |
| RBAC | Control de acceso basado en roles (Role-Based Access Control). |
| Comprobante | Documento (impreso o digital) que resume una transacción de cobro. |

## 12. Propuesta preliminar de stack tecnológico

Se adopta la arquitectura de referencia sugerida en el prompt maestro (sección 27), por ser tecnologías maduras, ampliamente soportadas y con buen ajuste al requisito de migración SQLite→MySQL sin reescritura de lógica:

| Capa | Tecnología | Motivo |
|---|---|---|
| Backend | Java + Spring Boot | Ecosistema maduro, DI nativa, fácil de testear, gran soporte de librerías empresariales. |
| API | REST | Simplicidad, ampliamente soportado por clientes web/móvil, fácil de documentar con OpenAPI. |
| Persistencia | Spring Data JPA + Repository Pattern | Abstrae el motor de base de datos; el dominio no depende de SQL específico. |
| Base de datos (desarrollo) | SQLite (archivo, no en memoria) | Cero configuración, ideal para un parqueadero pequeño y para desarrollo local. |
| Base de datos (producción futura) | MySQL | Motor maduro, ampliamente soportado, buena ruta de escalamiento. |
| Migraciones | Flyway | Versionado de esquema reproducible entre SQLite y MySQL. |
| Frontend | React + TypeScript + Vite | Ecosistema maduro, tipado estático reduce errores, Vite da arranque y build rápidos. |
| PWA | Vite PWA / Workbox | Integración directa con Vite, manejo de manifest y service worker estandarizado. |
| UI | **Tailwind CSS** | Confirmado por el usuario. Utility-first, sin componentes preconstruidos que limiten el diseño mobile-first de botones grandes/táctiles que exige el prompt; bundle más ligero que una librería de componentes completa, favorable para PWA. |
| Pruebas backend | JUnit + Mockito | Estándar de facto en el ecosistema Spring. |
| Pruebas frontend | Vitest + Testing Library | Integración nativa con Vite, enfoque en comportamiento sobre implementación. |
| E2E | Playwright | Soporte multi-navegador, buena estabilidad para flujos críticos (ingreso→salida→cobro). |
| Contenedores | Docker + Docker Compose | Empaqueta backend/frontend/MySQL futuro sin exigir MySQL durante desarrollo con SQLite. |

## 13. Propuesta de arquitectura

Arquitectura en capas (Clean Architecture aplicada sin sobreingeniería, acorde al tamaño del sistema):

```
UI / Presentation   (React + TS — pantallas, componentes, navegación)
        ↓
Application         (casos de uso / servicios de aplicación, DTOs)
        ↓
Domain               (entidades, reglas de negocio, motor de tarifas, puertos/interfaces)
        ↓
Infrastructure       (JPA repositories, adaptadores SQLite/MySQL, seguridad, time service)
```

Principios clave:

- **Repository Pattern**: el dominio depende de interfaces (puertos), no de JPA ni de SQL específico de un motor.
- **Motor de tarifas desacoplado**: reglas tarifarias como datos de configuración, evaluadas mediante Strategy (una estrategia por modalidad de tarifa), con Factory para seleccionar la estrategia aplicable y Chain of Responsibility para resolver prioridad cuando varias reglas coinciden.
- **Servicio de tiempo centralizado**: toda fecha/hora se obtiene de un único servicio (zona horaria configurable, por defecto `America/Bogota`), para evitar lógica de fechas dispersa y facilitar pruebas.
- **DTOs** entre Application y Presentation para no exponer entidades de dominio directamente en la API.
- **Auditoría transversal**: soft delete / estados (`ANULADO`, etc.) en vez de borrado físico de información transaccional.

## 14. Roadmap de las siguientes fases SDD

Conforme a la sección 38 del prompt maestro. Cada fase se inicia solo cuando el usuario lo indique explícitamente (no se avanza automáticamente):

| Fase | Contenido |
|---|---|
| Fase 1 — Especificación | Requisitos funcionales/no funcionales formales, casos de uso, reglas de negocio cerradas, criterios de aceptación, matriz de trazabilidad. |
| Fase 2 — Modelo y arquitectura | Modelo de dominio, diagrama de componentes, ERD, estrategia SQLite→MySQL, arquitectura PWA, estrategia de seguridad, contratos de API (OpenAPI). |
| Fase 3 — UX/UI | Mapa de navegación, user flows, wireframes, design system, pantallas mínimas (login, dashboard, ingreso, parqueados, cobro/salida, caja, historial, reportes, tarifas, usuarios, configuración). |
| Fase 4 — Foundation | Estructura de proyecto backend/frontend, seguridad base, persistencia SQLite, migraciones, manejo de errores, logging, configuración PWA. |
| Fase 5 — Operación principal | Ingreso, vehículos activos, salida, cálculo tarifario, pagos. |
| Fase 6 — Caja | Apertura, movimientos, cierre, arqueo. |
| Fase 7 — Administración | Usuarios, tarifas, configuración, auditoría. |
| Fase 8 — Reportes | Diario, semanal, mensual, personalizado. |
| Fase 9 — PWA y UX | Instalación, responsive, service worker, cache, experiencia móvil, accesibilidad básica, rendimiento. |
| Fase 10 — QA | Unitarias, integración, E2E, pruebas tarifarias, seguridad, responsive. |
| Fase 11 — Contenedorización | Dockerfiles, docker-compose, variables de entorno, documentación de despliegue. |
| Fase 12 — Preparación MySQL | Verificación de cambio de motor sin modificar lógica de dominio; documentación del procedimiento. |

## 15. Tabla de decisiones pendientes

Siguiendo el procedimiento exigido en la sección 10 del prompt (identificar → documentar → proponer alternativas → recomendar default → mantener parametrizable):

| Tema | Pregunta abierta | Alternativas | Recomendación por defecto | Estado |
|---|---|---|---|---|
| Horarios AM/PM | ¿Qué rangos horarios exactos definen "AM" y "PM"? | (a) Fijo 06:00–18:00 / 18:00–06:00; (b) Configurable por el administrador sin límite fijo | (b) Configurable, con valores semilla 06:00–18:00 (AM) y 18:00–06:00 (PM) | **Confirmado** — BR-010 |
| Fracciones | ¿Cómo se cobra una fracción de tiempo incompleta? | (a) Se cobra la fracción completa aunque no se cumpla; (b) Se prorratea proporcionalmente | (a) Fracción completa (estándar en la mayoría de parqueaderos), con unidad de fracción configurable (ej. 15/30/60 min) | **Confirmado** — BR-011 |
| Tolerancia | ¿Cuántos minutos de gracia se otorgan sin cobro, y sobre qué operación aplican? | (a) Sin tolerancia; (b) Tolerancia configurable aplicada solo a la primera fracción | (b) Tolerancia configurable por tarifa, por defecto 10 minutos, aplicada al cálculo total del cobro | **Confirmado** — BR-012 |
| Tarifa "todo el día" | ¿Qué condición activa esta tarifa (ingreso antes de cierto horario, permanencia mínima, etc.)? | (a) Se activa automáticamente si el costo por tiempo superaría el valor fijo; (b) Se activa solo si el ingreso ocurre dentro de una franja configurada | (b) Activación por franja horaria de ingreso configurable, con comparación automática de costo como regla secundaria opcional | **Confirmado** — BR-013 |
| Tarifas carro vs. moto | ¿Las tarifas son independientes por tipo de vehículo desde el MVP? | (a) Mismas tarifas para ambos tipos; (b) Tarifas independientes por tipo de vehículo desde el inicio | (b) Tarifas independientes por tipo de vehículo desde el MVP (el modelo de tarifas ya incluye `tipo_vehiculo` como atributo) | **Confirmado** — BR-005 |
| Cruce de medianoche | ¿Cómo se calcula el cobro cuando el parqueo abarca dos días calendario? | (a) Se trata como tiempo continuo sin distinción de día; (b) Se recalcula tarifa por cada día calendario cruzado | (a) Tiempo continuo (más simple y predecible para el cliente), documentado explícitamente en el motor tarifario | **Confirmado** — BR-014 |
| Permanencias > 24h | ¿Cómo se tarifica una permanencia que supera las 24 horas? | (a) Se aplican múltiplos de la tarifa "todo el día"; (b) Se limita a un valor máximo diario configurable | (a) Múltiplos de tarifa "todo el día" por cada día completo, más tarifa por tiempo para el remanente | **Confirmado** — BR-015 |
| Capacidad máxima | ¿Se configura un límite de cupos desde el MVP y qué ocurre al llegar al límite? | (a) Sin límite (solo informativo); (b) Límite configurable que bloquea nuevos ingresos; (c) Límite configurable solo como alerta, sin bloqueo | (c) Límite configurable, usado en el dashboard como "Ocupados/Capacidad", sin bloquear el ingreso en el MVP | **Confirmado** — BR-016 |
| Impresión de comprobantes | ¿Se requiere impresión térmica dedicada o basta impresión estándar del navegador? | (a) Impresión térmica directa (requiere integración de hardware); (b) Vista imprimible estándar (navegador / PDF) | (b) Vista imprimible estándar en el MVP; impresión térmica queda fuera de alcance inicial | **Confirmado** — BR-017 |
| Medios de pago | ¿Qué medios de pago existen desde el MVP y son fijos o parametrizables? | (a) Lista fija de 3 medios; (b) Catálogo parametrizable desde configuración | (b) Catálogo parametrizable, con semilla: efectivo, transferencia, otros | **Confirmado** — BR-018 |
| Manejo offline | ¿Qué tanto debe funcionar la app sin conexión en el MVP? | (a) Solo shell de la app (recursos estáticos) disponible offline, sin operaciones transaccionales; (b) Cola de operaciones offline con sincronización posterior | (a) Solo offline de recursos estáticos en el MVP; offline transaccional queda documentado como evolución futura | **Confirmado** — BR-019 |

**Librería UI confirmada: Tailwind CSS** (2026-09-22). Ver `specs/decisions/ADR-001-stack-tecnologico.md`.

---

## Próximo paso sugerido

~~Con este documento aprobado..., la siguiente iteración sería iniciar la Fase 1 — Especificación.~~ **Completado.** Ver `01-functional-requirements.md`, `02-non-functional-requirements.md`, `04-use-cases.md`, `05-business-rules.md`, `12-traceability-matrix.md`.

**Fase 2 — Modelo y Arquitectura: completada** (2026-09-22). Ver `03-domain-model.md`, `06-architecture.md`, `07-data-model.md`, `08-api-contract.md` (+ `openapi.yaml`), `10-security-specification.md` y los ADR en `decisions/`.

**Fase 3 — UX/UI: completada** (2026-09-22). Ver `09-ux-specification.md` (mapa de navegación, 5 user flows, design system sobre Tailwind, 10 componentes reutilizables, wireframes de las 11 pantallas mínimas + Auditoría, reglas responsive).

**Fase 4 — Foundation: completada** (2026-09-22). Primera fase con código real. Entregado y verificado end-to-end (backend levantado, pruebas de integración pasando, login probado en navegador real):

- Backend: Java 21 + Spring Boot 4.1.1, estructura en capas (`presentation/application/domain/infrastructure`) según `06-architecture.md`.
- Persistencia: esquema completo (`db/migration/V1__init.sql`) vía Flyway sobre SQLite (perfil `development`), con datos semilla solo en `development`/`test` (`db/seed/V2__seed_data.sql`).
- Seguridad: autenticación por sesión con cookie `httpOnly` + CSRF (`decisions/ADR-004-autenticacion.md`), RBAC base, contraseñas BCrypt, manejo centralizado de errores sin fuga de detalles internos.
- Frontend: React + TypeScript + Vite + Tailwind CSS, PWA instalable (manifest + service worker, cache-first solo en recursos estáticos), navegación por rol (`09-ux-specification.md` §1) con los ítems aún no implementados visualmente deshabilitados.
- Pruebas: `AuthControllerTest` (4 casos) verde sobre SQLite en memoria.
- Decisiones técnicas imprevistas del ecosistema (Spring Boot 4, Flyway 12, Vite 8→6) documentadas en `decisions/ADR-005-notas-modularizacion-spring-boot-4.md`.
- Instrucciones de ejecución: `README.md`.

**Fase 5 — Operación principal: completada** (2026-09-22). Ingreso, vehículos activos, salida, motor de cálculo tarifario y pagos — verificados con pruebas automatizadas (34 pruebas backend, todas en verde) y manualmente end-to-end (curl a través del proxy de Vite, el mismo camino que usa la SPA).

- Motor de tarifas (RF-043 a RF-048, BR-005 a BR-016): Strategy + Factory + resolución por prioridad (decisions/ADR-003), con batería de 25 pruebas de borde (tolerancia, fracciones y redondeo, franjas AM/PM con cruce de medianoche, activación de "todo el día", empates de prioridad — mecanismo de BR-013 verificado — y permanencias >24h, BR-015).
- Ingreso (RF-023–029), consulta de activos (RF-030–033), salida/cobro atómico (RF-034–042, RF-049–051) — con idempotencia (BR-021) y unicidad de placa activa (BR-001) verificadas.
- Apertura de caja (RF-052/053) implementada como dependencia técnica mínima de Fase 5 (Payment.cashSessionId es NOT NULL en el modelo aprobado) — el cierre/arqueo completo queda para Fase 6, documentado en `CashSessionService`.
- Frontend: pantallas reales de Nuevo ingreso, Vehículos parqueados y Cobro/salida (wireframes `09-ux-specification.md` §5.3–5.5), conectadas a la API real (sin datos simulados).
- **Decisiones técnicas relevantes durante la implementación:** se descubrió y corrigió una incompatibilidad real del dialecto community de Hibernate para SQLite con columnas `DATE`/`TIME` (no es un problema de formato de datos, sino del driver JDBC) — documentada en `decisions/ADR-005-notas-modularizacion-spring-boot-4.md`; se cerró la ambigüedad de BR-011/BR-012/BR-013 con un algoritmo concreto (ver `05-business-rules.md`).
- Verificación visual en navegador no fue posible en esta iteración (la extensión de Chrome no conectó); se compensó con pruebas de integración automatizadas más una verificación manual exhaustiva de cada endpoint a través del proxy de desarrollo del frontend.

**Fase 6 — Caja: completada** (2026-09-23). Cierre y arqueo, sobre la apertura ya entregada en Fase 5.

- Cerrado: BR-023.1 (algoritmo de arqueo precisado) — efectivo esperado = saldo inicial + cobros en efectivo (los electrónicos/otros no afectan el físico); diferencia = reportado − esperado. Totales congelados al cerrar.
- Se agregó `PaymentMethod.category` (CASH/ELECTRONIC/OTHER) vía `V3__payment_method_category.sql` — vacío real detectado al implementar: no había forma data-driven de saber qué medios de pago son efectivo físico.
- Corrección administrativa de un cierre (RF-057/BR-023), con motivo obligatorio y registro de auditoría — para esto se construyó una capacidad mínima de auditoría (`AuditService`, solo escritura; la consulta completa es Fase 7).
- RBAC verificado: solo el empleado dueño de la caja (o un Administrador) puede cerrarla; solo un Administrador puede corregir un cierre ya finalizado.
- Frontend: pantalla de Caja (`09-ux-specification.md` §5.6) — abrir si no hay caja activa, resumen en vivo, formulario de cierre con el conteo físico.
- 3 pruebas de integración nuevas (`CashSessionCloseIntegrationTest`); 37 pruebas backend en total, todas en verde. Verificado manualmente end-to-end en SQLite de archivo y a través del proxy de Vite.
- Verificación visual en navegador tampoco fue posible esta iteración (extensión de Chrome sin conectar, intentado varias veces); compensado igual que en Fase 5 con pruebas automatizadas + verificación manual exhaustiva por el mismo camino que usa la SPA.

**Fase 7 — Administración: completada** (2026-09-23). Usuarios, tarifas, configuración y auditoría.

- **Usuarios** (RF-005–009): crear, activar/desactivar, cambio de contraseña (propio y por administrador), con hash BCrypt y auditoría de cada cambio.
- **Tarifas** (RF-018–022): CRUD completo con validación de consistencia por modalidad (RF-021 — FULL_AM/FULL_PM/FULL_DAY exigen horario, BY_TIME exige fracción), auditando cada creación/edición/activación. La escritura de `startTime`/`endTime`/`validFrom`/`validTo` (mapeados como texto por el hallazgo de Fase 5) se verificó explícitamente end-to-end, incluida su reutilización inmediata por el motor de cálculo.
- **Configuración** (RF-010–015): edición de datos del parqueadero, más los catálogos de tipos de vehículo y medios de pago (ahora con `category`) parametrizables sin cambios de código.
- **Auditoría** (RF-066–068): se completó la consulta (`GET /api/audit-logs`, antes solo existía la escritura de Fase 6) y se agregó la anulación administrativa de sesiones de parqueo (UC-011) con motivo obligatorio y registro de auditoría.
- Frontend: pantallas de Usuarios, Tarifas, Configuración y Auditoría (`09-ux-specification.md` §5.9–5.11 + Auditoría), conectadas a la API real.
- **Bug real encontrado y corregido en las pruebas mismas** (no en el código de producción): una prueba de integración con concatenación de strings multilínea aplicaba `.formatted()` solo al último fragmento por precedencia de operadores en Java, dejando `"%s"` sin sustituir — quedó documentado como recordatorio para futuras pruebas.
- 4 pruebas de integración nuevas (`AdminIntegrationTest`); 41 pruebas backend en total, todas en verde. Verificado manualmente end-to-end (creación, edición, activación, PATCH/PUT/POST) a través del proxy de Vite, incluida una tarifa creada por el administrador siendo resuelta correctamente por el motor de cálculo en una sesión real.
- Verificación visual en navegador nuevamente no fue posible (extensión de Chrome sin conectar pese a varios intentos); compensado con la misma disciplina de pruebas automatizadas + verificación manual exhaustiva de las Fases 5 y 6.

**Fase 8 — Reportes: completada** (2026-09-23). Reportes (diario/semanal/mensual/rango personalizado) y Dashboard del Administrador.

- **Reportes** (RF-060–063): `ReportService` agrega, por streams (no SQL `GROUP BY`, por portabilidad SQLite→MySQL), vehículos ingresados/finalizados/anulados, recaudo total y desglosado por tipo de vehículo, medio de pago, tarifa y empleado, duración promedio, hora de mayor movimiento y suma de diferencias de caja del rango. `GET /api/reports/daily?date=` y `GET /api/reports/summary?from=&to=`, protegidos con `@PreAuthorize("hasRole('ADMIN')")`.
- **Dashboard** (RF-064–065): `DashboardService` calcula el estado del día actual (zona horaria de la configuración) — ocupación vs. capacidad máxima, ingresos/salidas/recaudo de hoy, cajas abiertas y los últimos movimientos. `GET /api/dashboard`, mismo RBAC.
- Se extendieron los repositorios de dominio (`ParkingSessionRepository`, `PaymentRepository`, `CashSessionRepository`) con consultas por rango de fecha/hora necesarias para ambos servicios, sin acoplar la lógica de agregación a JPA.
- Frontend: pantalla de Reportes (`09-ux-specification.md` §5.8) con selector Diario/Semanal/Mensual/Rango y desgloses; el Home del Administrador ahora es un Dashboard real (antes placeholder) que se refresca cada 30s.
- 2 pruebas de integración nuevas (`ReportAndDashboardIntegrationTest`, con aserciones por delta antes/después dado que la base SQLite en memoria se comparte entre toda la suite): flujo completo (caja, dos cobros con medios de pago distintos, una anulación, cierre de caja con diferencia) reflejado correctamente en reporte y dashboard; RBAC (empleado recibe 403). 43 pruebas backend en total, todas en verde.
- **Verificación visual en navegador realizada por primera vez en el proyecto** (la extensión de Chrome conectó exitosamente): se validó en vivo el ciclo completo — apertura de caja, nuevo ingreso, salida/cobro, cierre de caja con arqueo — confirmando que el Dashboard y Reportes reflejan la actividad real en tiempo real (antes y después de cada operación). Se detectó que los clics por coordenadas/referencia de la herramienta de automatización de navegador no llegaban al botón de envío del formulario de apertura de caja (aterrizaban en el input contiguo); se confirmó con `element.click()` vía JavaScript que el `onSubmit` de React funciona correctamente — **no es un defecto de la aplicación**, sino una limitación puntual de la herramienta de automatización en esta sesión.

**Fase 9 — PWA y UX: completada** (2026-09-23). Fase de validación (prompt.md §Fase 9): instalación, responsive, service worker, cache, experiencia móvil, accesibilidad básica y rendimiento — sobre la base PWA ya construida en Fase 4.

- **Instalación (RNF-005/008):** manifest válido verificado en el navegador (`fetch` real del `manifest.webmanifest` servido) — nombre, iconos 192/512 + 512 maskable, `display: standalone`, service worker activo y controlando la página. Cumple los criterios de instalabilidad de Chrome.
- **Service worker y cache (RNF-007):** se confirmó por inspección de `caches` en el navegador que Workbox precachea el shell completo (HTML, JS, CSS, iconos, manifest — 9 entradas / ~321 KiB); una recarga sirve el shell con `transferSize: 0` (desde cache). La regla `NetworkOnly` para `/api/**` se mantiene intacta (sin cache de datos transaccionales, BR-019).
- **Gap real encontrado y corregido (RNF-007):** un fallo de red (offline) no producía un aviso claro — `fetch()` lanzaba un `TypeError` genérico que cada pantalla mostraba con su mensaje de error por defecto, indistinguible de un error de servidor. Se corrigió en `api/client.ts` envolviendo el `fetch` y traduciendo cualquier fallo de red a un `ApiError` con mensaje explícito ("Sin conexión a internet..."), reutilizando el manejo de errores ya existente en cada pantalla sin tocarlas una por una.
- **Accesibilidad básica:** se corrigió `<html lang="en">` → `lang="es"` (index.html) y se alineó `manifest.lang` a `es` — la app es enteramente en español y el idioma declarado afecta lectores de pantalla. Se confirmó por revisión de código: objetivos táctiles ≥44px (`min-h-11`) en todos los controles interactivos, labels asociados a inputs vía `htmlFor`/`id` en todos los formularios, sin imágenes sin texto alternativo (los íconos son emoji con texto acompañante, no imágenes), estados de foco visibles (`focus:ring-2`) en los campos de login.
- **Responsive (RNF-006):** validado por revisión de código — ningún componente usa anchos fijos en píxeles ni tablas (que requerirían scroll horizontal); todos los formularios usan contenedores fluidos (`max-w-*` + `mx-auto`); las grillas de estadísticas usan `grid-cols-2`/`sm:grid-cols-3`, apropiadas desde smartphone; navegación con `Sidebar` (oculto en móvil) y `BottomNav` (oculto en escritorio) con menú "Más" para ítems adicionales. **Limitación de la sesión:** la herramienta de redimensionado de ventana del navegador conectado no aplicó el tamaño solicitado (permaneció en 1536×756/1920×889 pese a solicitar 390×844 y 400×850 en dos pestañas distintas) — no fue posible capturar una comprobación visual real en viewport móvil; la validación de esta iteración se apoya en la revisión de código más las capturas de escritorio ya verificadas visualmente en la Fase 8. Queda como pendiente recomendado para la Fase 10 (QA) verificar visualmente en un dispositivo o emulador real.
- **Rendimiento (RNF-003):** build de producción limpio — bundle 310 KB JS (93 KB gzip), 17 KB CSS (4.3 KB gzip), sin code-splitting adicional necesario a esta escala. Medido con la Performance API del navegador sobre el build de producción: `DOMContentLoaded` 55 ms, `load` 149 ms — muy por debajo del umbral de 2s de RNF-003.
- No se agregó código nuevo de funcionalidad (fase de validación, no de features) salvo las dos correcciones puntuales de accesibilidad/manejo de errores de red descritas arriba.

**Fase 10 — QA: completada** (2026-09-24). Ejecutar pruebas unitarias, de integración, E2E, tarifarias, de seguridad y responsive (prompt.md §Fase 10).

- **Unitarias/integración/tarifarias (backend):** ya extensamente cubiertas en fases anteriores (motor de tarifas: 25 casos de borde en 3 clases; 8 clases de integración por feature). Confirmadas en verde en esta fase sin cambios: **49 pruebas backend, todas en verde** (43 previas + 6 nuevas de seguridad).
- **Seguridad (backend, nuevo — `SecurityIntegrationTest`, 6 pruebas):** barrido de RBAC contra endpoints representativos de cada nivel declarado (público, ADMIN+EMPLOYEE, solo ADMIN) verificando 401 sin sesión y 403 con rol insuficiente; rechazo de una mutación sin token CSRF; verificación de que la contraseña se almacena como hash BCrypt (nunca en texto plano, RNF-009); verificación de que un JSON malformado no filtra detalles internos (stack trace, nombres de paquete, RNF-012).
- **Hallazgo técnico real durante esta fase:** `SecurityMockMvcRequestPostProcessors.csrf()` (usado en toda la suite para simular un token válido) resultó incompatible con `CookieCsrfTokenRepository` de carga diferida cuando se intentó probar el flujo *real* de doble envío CSRF vía MockMvc — investigado exhaustivamente y confirmado como una interacción conocida de la herramienta de pruebas, no un defecto de la aplicación (el servidor real, verificado por `curl`, funciona correctamente en cada petición). Documentado en detalle en `decisions/ADR-005-notas-modularizacion-spring-boot-4.md` (hallazgo #8). La propiedad de seguridad esencial (ausencia de token → 403) se prueba de forma robusta con MockMvc; el flujo positivo completo se verificó manualmente.
- **Unitarias/componentes (frontend, nuevo):** se incorporó Vitest + Testing Library (no existía tooling de pruebas en el frontend hasta ahora). **16 pruebas, todas en verde**: `lib/format.ts` (formato de moneda/duración/hora), `layout/navigation.ts` (reglas de menú por rol y de la barra inferior móvil), `layout/NavLinkItem.tsx` (accesibilidad de items deshabilitados) y `api/client.ts` (el aviso de "sin conexión" agregado en la Fase 9, el header CSRF en mutaciones, y la propagación de errores del servidor).
- **E2E y responsive (frontend, nuevo — Playwright):** se configuró Playwright (`playwright.config.ts`, proyectos `desktop`/`mobile` con emulación de Pixel 7) y se escribieron `e2e/golden-path.spec.ts` (login → apertura de caja → ingreso → cobro → reflejo en Dashboard y Reportes, con placa única por corrida para ser repetible contra el backend real) y `e2e/responsive.spec.ts` (sin scroll horizontal en 8 pantallas clave en viewport móvil, barra inferior móvil reemplazando a la barra lateral, objetivos táctiles ≥44px) — type-checados sin errores (`tsconfig.e2e.json`). **Limitación honesta de esta sesión:** el entorno de ejecución bloqueó la descarga del binario de Chromium de Playwright (timeout de red consistente contra `cdn.playwright.dev`), por lo que estas pruebas **no se ejecutaron** en esta iteración — quedan listas para correr (`npm run test:e2e`) en un entorno con acceso de red completo o en CI. El flujo dorado que codifican fue verificado manualmente end-to-end en navegador real en la Fase 8 (sin cambios de comportamiento relevantes desde entonces); la verificación responsive queda pendiente de una ejecución real (recomendado como primer paso de la Fase 11, o en cuanto se disponga de red).
- No se modificó lógica de negocio en esta fase; los únicos cambios de código de producción fueron los dos de la Fase 9 (accesibilidad y aviso de conexión), ya cubiertos por pruebas.

**Fase 11 — Contenedorización: completada** (2026-09-24). Dockerfiles, docker-compose, variables de entorno y documentación de despliegue (prompt.md §Fase 11).

- **`backend/Dockerfile`** — build multi-etapa (`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`), usuario no-root, `VOLUME /app/data` (perfil `development`/SQLite, coincide con la ruta relativa `./data/parking.db` de `application-development.yml`), `HEALTHCHECK` contra `/actuator/health`.
- **`frontend/Dockerfile`** — build multi-etapa (`node:22-alpine` → `nginx:1.27-alpine`), sirve los estáticos generados por `vite build` (incluye manifest/service worker de la PWA de la Fase 9). `frontend/nginx.conf` añade fallback de rutas para React Router (`try_files $uri /index.html`) y reenvía `/api/**` al servicio `backend` — el navegador ve un solo origen, igual que el proxy de Vite en desarrollo, así que el mecanismo de CSRF de doble envío no necesita ningún cambio.
- **`docker-compose.yml`** (perfil `development`, SQLite) y **`docker-compose.prod.yml`** (perfil `production`, MySQL — agrega un servicio `mysql:8.4` y lee `DB_*`/`CORS_ALLOWED_ORIGINS`/`PARKING_TIMEZONE` de `.env`, reutilizando exactamente las variables ya documentadas en `.env.example` desde fases anteriores). Puertos publicados elegidos para no chocar con procesos de desarrollo ya corriendo en esta máquina (8080/5173) ni con otros contenedores ajenos al proyecto — ver comentarios en cada archivo.
- **Verificación real de extremo a extremo en esta sesión** (Docker estaba disponible): build de ambas imágenes exitoso; `docker compose up` con ambos healthchecks en verde; `GET /api/health` directo y a través del proxy de nginx; **login real (POST /api/auth/login) con el flujo completo de doble envío CSRF a través del proxy de nginx**, confirmando que el mecanismo de la Fase 4/10 funciona igual contenedorizado; fallback de rutas de la SPA (`/reportes` sin 404); `manifest.webmanifest` y `sw.js` servidos correctamente; y persistencia de los datos de SQLite en el volumen con nombre tras reiniciar el contenedor del backend (`docker compose restart backend` → mismo archivo `parking.db`, mismo tamaño, arranque limpio). `docker-compose.prod.yml` se validó estructuralmente con `docker compose config` (interpolación de variables desde `.env.example` correcta); la verificación funcional completa contra MySQL real queda para la Fase 12 según el propio alcance de esa fase.
- Documentación de despliegue agregada a `README.md` (sección "Despliegue con Docker").

**Trabajo entre fases (2026-09-28):** rediseño UI/UX (ver `09-ux-specification.md` §3.4) y nueva regla BR-008.1 (tarifa alterna al cobrar, con validación de que el valor cobrado sea el calculado), a pedido del usuario. Detalle en `README.md`.

**Fase 12 — Preparación MySQL: completada** (2026-09-29). Comprobar el cambio SQLite → MySQL sin modificar la lógica de dominio y documentar el procedimiento (prompt.md §Fase 12). Detalle completo, evidencia y procedimiento en `decisions/ADR-006-migracion-mysql.md`.

- **Resultado:** la aplicación funciona sobre MySQL 8.4 **sin ningún cambio en el código Java** de producción. Suite del backend 57/57 en ambos motores (`mvn test` sobre SQLite y `scripts/verify-mysql.sh` sobre MySQL, mismo código, solo cambia el datasource); suite E2E de la interfaz 12/12 contra el stack `docker-compose.prod.yml` con MySQL.
- **Hallazgos corregidos (solo configuración/migraciones/infraestructura):** (1) MySQL descartaba en silencio las FK "en línea" de V1 → migración exclusiva de MySQL `db/vendor/mysql/V3_1` con las 18 FK; (2) `TIMESTAMP` de MySQL (límite 2038, sin milisegundos) → `DATETIME(6)` en la misma V3_1; (3) el perfil `test` no activaba `foreign_keys` en SQLite → activado, más `SchemaIntegrityTest`; (4) el perfil `production` no conectaba a MySQL 8 sin SSL → `DB_ALLOW_PUBLIC_KEY_RETRIEVAL`; (5) los dos compose compartían nombre de proyecto → `name: parking-prod`.
- **Migraciones por motor:** `classpath:db/vendor/{vendor}` en los tres perfiles, con convención de numeración documentada en ADR-006.
- **Corrección de conteo:** los totales de pruebas del backend reportados en la sesión del 2026-09-28 ("53" y "60") estaban inflados por reportes de Surefire viejos; los reales eran 49 (inicio) y 56 (tras BR-008.1). Hoy: 57.

**Decisiones pendientes (usuario):** (a) ~~cómo cargar en producción los datos de referencia y crear el primer administrador~~ **resuelta el 2026-09-29 (opción recomendada, elegida por el usuario): ver `decisions/ADR-007-arranque-produccion.md`**: migración común V4 con los datos de referencia (idempotente, portable) y primer administrador creado al arrancar desde variables de entorno, sin credenciales por defecto. Verificado en una instalación de producción nueva sobre MySQL (primer uso completo por API; reinicio, falta de variables y contraseña inválida), con la suite del backend en 62/62 en ambos motores; (b) si se necesitará migrar datos existentes de SQLite a MySQL: sigue pendiente.

**Correcciones posteriores (2026-09-29):** errores del cliente con su código HTTP correcto en lugar de 500 (`GlobalExceptionHandler`, 4 pruebas nuevas en `ErrorHandlingIntegrationTest`, ver `10-security-specification.md`); manifest de la PWA servido por nginx como `application/manifest+json`; healthcheck del contenedor del frontend corregido (usaba `localhost`, que dentro del contenedor resuelve a IPv6).

**Bug de concurrencia encontrado al correr el E2E contra producción (2026-09-29):** el número de operación de los pagos se calculaba como `COUNT + 1`; con dos cobros simultáneos en MySQL ambos obtenían el mismo número y uno fallaba con 500 (reproducido: 5 de 12 cobros concurrentes). No se veía en SQLite porque allí las escrituras van en serie. Corregido con un contador atómico (`V5__operation_counter.sql`, `PaymentRepository#nextOperationSequence`); verificado con 12/12 cobros concurrentes, números consecutivos sin repetidos, y la suite E2E 12/12 (dos corridas) contra el stack de producción. Las credenciales del E2E ahora pueden venir de `E2E_ADMIN_USERNAME` / `E2E_ADMIN_PASSWORD`, para correrlo contra producción con el administrador inicial y sin datos de prueba. Suite del backend: 67/67 en SQLite y en MySQL.

**Mejoras a pedido del usuario (2026-09-29):** sección "Tarifa" siempre visible al cobrar (BR-008.1); la auditoría de edición de tarifas guarda todos los campos de cálculo, incluido el horario anterior y el nuevo (BR-025); exportación de reportes a Excel y PDF (RF-063, `decisions/ADR-008-exportacion-reportes.md`). Suite del backend 71/71 en SQLite y MySQL; frontend 19/19; E2E 12/12. `openapi.yaml` documenta ahora los reportes (`/reports/daily`, `/reports/summary`, `/reports/summary/export`) y el dashboard, con sus esquemas `Report`, `Dashboard` y `RecentMovement`, y se corrigió el nombre de la cookie de sesión (`JSESSIONID`, antes decía `PARKING_SESSION`); validado con Redocly CLI (sin errores). Después se formalizaron los 14 endpoints restantes (usuarios, contraseñas, configuración, tipos de vehículo, medios de pago, auditoría, comprobante y corrección de cierre de caja), tomando sus formas de los DTO reales y verificando su comportamiento contra la app en ejecución: `openapi.yaml` cubre hoy todos los endpoints implementados. `GET /parking-sessions` (Historial) quedó marcado como diseño no implementado (`x-implemented: false`). Hallazgo: `CONFIGURATION_UPDATED` se auditaba sin valor anterior ni nuevo (ambos `null`), incumpliendo BR-025; **corregido el mismo día a pedido del usuario**: ahora guarda todos los campos antes y después, como las tarifas (helper compartido `AuditJson`, verificado en `AdminIntegrationTest`, 71/71 en SQLite y MySQL).

**Revisión de seguridad OWASP Top 10 (2026-09-29):** 12 hallazgos (3 altos: sesión de usuario desactivado seguía activa, session fixation, Tomcat con CVE críticos); 11 corregidos y verificados en vivo, el de HTTPS queda para el despliegue. Detalle en `10-security-specification.md` §"Revisión OWASP". Suite del backend 82/82 en SQLite y MySQL; frontend 19/19 y 0 vulnerabilidades en `npm audit`; E2E 12/12 con la nueva CSP.

**Pendiente suspendido por el usuario (2026-09-29) — instalación de la PWA en el celular:** la app es instalable (chequeo `Page.getInstallabilityErrors` de Chrome sin errores en `http://localhost:8081`), pero desde el celular se accede por `http://<IP-local>:8081` y Chrome la rechaza con `not-from-secure-origin`: una PWA solo se instala desde HTTPS (o localhost). Además, el firewall de Windows (perfil de dominio) no tiene reglas que abran el puerto 8081. Opciones evaluadas: HTTPS local con certificado (mkcert), marcar el origen como seguro en `chrome://flags` (solo Android, sin cambios al proyecto; elegida y luego suspendida) o túnel HTTPS público (descartado: expondría usuarios de prueba). En producción se resuelve con dominio y HTTPS real.

**Siguiente paso:** según `prompt.md`, la Fase 12 es la última del roadmap. Pendiente de instrucción del usuario (en particular, las decisiones (a) y (b)).

**Cierre de brechas funcionales (2026-09-29):** tras medir 89,4 % de cobertura de RF, se completaron los pendientes:
Historial con filtros (RF-058), operaciones del turno con acceso al comprobante (RF-059), comprobante imprimible
con formato configurable (RF-050, RF-015; se corrigió además que guardar la configuración borraba
`receiptFormatJson`), cambio de contraseña propia y de otros usuarios en la UI (RF-007, RF-008), descripción,
tiempo mínimo y vigencia final en tarifas (RF-018), filtro por tipo y orden en Parqueados (RF-032), anulación
con motivo desde Parqueados (RF-068), consulta y corrección de cajas cerradas (RF-057), auditoría del cierre de
caja `CASH_SESSION_CLOSED` (RF-066), filtros de auditoría por usuario y fechas (RF-067) y exportación CSV.
También se corrigió que las fechas "hoy" del frontend usaban UTC (después de las 19:00 en Bogotá daban el día
siguiente). Verificación: backend 90/90 en SQLite y MySQL 8.4; frontend 22/22; E2E 18/18 (+12 omitidos por
proyecto) contra el stack Docker, con 2 flujos nuevos (`e2e/full-coverage.spec.ts`).

**Comprobante de ingreso y configuración de impresión (2026-10-05):** nuevo RF-072 y BR-027. Cada ingreso recibe un
consecutivo `ING-000001` (migración V6, contador atómico como el de los pagos). El comprobante de ingreso
(`/comprobantes-ingreso/{id}`) muestra los datos del parqueadero, el número, la placa, el tipo de vehículo, la
fecha y hora, las observaciones y, según el formato, el empleado que registró, además de la leyenda "Conserve
este comprobante para retirar su vehículo". En Configuración, el administrador elige si se ofrece imprimir el
comprobante al ingreso (por defecto no) y/o a la salida (por defecto sí, como antes); desactivado, no aparece el
botón. Historial permite reimprimir ambos comprobantes.

**Respaldo completo de la base de datos (2026-10-06):** nuevo RF-073 y ADR-009. El administrador exporta, desde
**Respaldo**, un JSON portable con todas las tablas y lo importa en otra ejecución. La importación reemplaza todo de
forma atómica y exige la misma versión de esquema y un administrador activo. Verificado con
`BackupIntegrationTest` (backend 98/98 en SQLite y MySQL), pruebas unitarias del frontend (26/26), E2E 21/21 y una
transferencia real de la instalación SQLite a una MySQL temporal, con conteos y montos idénticos.

**Mensualidades (2026-10-19):** RF-074 a RF-077, BR-028 a BR-031 (enmienda de BR-020) y ADR-010.
- El administrador crea mensualidades con el valor y el día de corte pactados y las placas autorizadas.
- El ingreso de una placa autorizada se reconoce solo; se admite un vehículo a la vez.
- La salida es sin cobro mientras la mensualidad esté al día o en días de gracia (configurables); vencida, se cobra con las tarifas normales.
- El pago mensual se cobra en caja: cuenta en el arqueo y los reportes, usa el consecutivo OP y genera comprobante.
- Migraciones V7 y V7_1 (MySQL).
- Verificado: backend 106/106 en SQLite y MySQL y frontend 28/28.
