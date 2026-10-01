# PROMPT MAESTRO — DESARROLLO SDD DE UNA PWA PARA GESTIÓN DE PARQUEADERO

Actúa como un equipo senior de desarrollo de software compuesto por:

* Arquitecto de software.
* Desarrollador full-stack senior.
* Especialista en Progressive Web Applications (PWA).
* Especialista UX/UI.
* Analista de requisitos.
* Especialista en Specification-Driven Development (SDD).
* Diseñador de bases de datos.
* Ingeniero de pruebas y QA.
* Especialista en seguridad de aplicaciones.
* DevOps engineer.
* Experto funcional en sistemas de administración y operación de parqueaderos.

Tu objetivo es ayudarme a diseñar y desarrollar progresivamente una **aplicación web progresiva (PWA) para administrar un parqueadero pequeño**, utilizando una metodología **Specification-Driven Development (SDD)**.

La aplicación debe diseñarse pensando en simplicidad operativa, rapidez en el registro de vehículos, facilidad de aprendizaje para los empleados y capacidad de evolucionar posteriormente hacia una solución más completa.

---

# 1. PRINCIPIO FUNDAMENTAL DE DESARROLLO

NO desarrolles toda la aplicación inmediatamente.

El proyecto debe realizarse mediante **Specification-Driven Development (SDD)**.

Todo módulo debe seguir obligatoriamente este flujo:

**Necesidad → Requisitos → Especificación → Casos de uso → Reglas de negocio → Criterios de aceptación → Diseño → Contratos → Implementación → Pruebas → Validación**

Antes de escribir código de una funcionalidad, deben existir:

1. Especificación funcional.
2. Requisitos funcionales.
3. Requisitos no funcionales.
4. Casos de uso.
5. Historias de usuario cuando corresponda.
6. Reglas de negocio.
7. Criterios de aceptación.
8. Modelo de datos afectado.
9. Interfaces o contratos necesarios.
10. Escenarios de error.
11. Pruebas esperadas.

Cada especificación debe poder convertirse en una fuente verificable para comprobar que la implementación cumple exactamente lo solicitado.

---

# 2. OBJETIVO DEL SISTEMA

Construir una PWA para gestionar la operación diaria de un parqueadero pequeño.

La aplicación debe permitir:

* registrar ingreso de vehículos;
* registrar salida;
* identificar vehículos actualmente estacionados;
* calcular automáticamente el valor a pagar;
* gestionar diferentes modalidades de tarifa;
* registrar pagos;
* controlar operaciones realizadas por empleados;
* consultar historial;
* generar reportes;
* administrar usuarios;
* administrar configuración;
* manejar apertura y cierre de caja;
* funcionar correctamente desde computador, tablet o teléfono;
* poder instalarse como PWA;
* permitir una futura evolución de SQLite hacia MySQL sin modificar la lógica de negocio.

La prioridad de UX debe ser que operaciones como:

**INGRESO → BÚSQUEDA → SALIDA → COBRO**

requieran la menor cantidad posible de acciones.

---

# 3. TIPOS DE USUARIO

El sistema tendrá inicialmente dos roles.

## ADMINISTRADOR

Puede:

* gestionar usuarios;
* crear empleados;
* activar/desactivar usuarios;
* cambiar contraseñas;
* configurar datos del parqueadero;
* configurar tarifas;
* modificar tarifas;
* consultar operaciones;
* generar reportes;
* consultar cierres de caja;
* consultar auditorías;
* corregir operaciones cuando esté autorizado;
* anular registros dejando trazabilidad;
* configurar parámetros generales de la aplicación.

## EMPLEADO

Puede:

* iniciar sesión;
* registrar ingreso de vehículos;
* consultar vehículos estacionados;
* registrar salida;
* calcular cobro;
* registrar pago;
* visualizar información de su turno;
* realizar apertura de caja;
* realizar cierre de caja;
* consultar operaciones permitidas de su propio turno.

No debe poder modificar configuraciones administrativas.

Implementar control de acceso basado en roles: **RBAC**.

---

# 4. MÓDULOS DEL SISTEMA

Diseñar como mínimo los siguientes módulos:

1. Autenticación.
2. Usuarios y roles.
3. Configuración del parqueadero.
4. Tipos de vehículo.
5. Tarifas.
6. Registro de ingreso.
7. Vehículos actualmente estacionados.
8. Registro de salida.
9. Motor de cálculo tarifario.
10. Gestión de cobros.
11. Caja.
12. Historial de operaciones.
13. Reportes.
14. Dashboard.
15. Auditoría.
16. Configuración PWA.
17. Persistencia.
18. Respaldo y recuperación futura.

Diseñar la arquitectura para permitir agregar posteriormente módulos adicionales.

---

# 5. VEHÍCULOS

La aplicación debe soportar inicialmente:

* automóviles;
* motocicletas.

La arquitectura debe permitir crear nuevos tipos de vehículos posteriormente sin modificar significativamente la lógica existente.

Cada ingreso debe contener como mínimo:

* ID único.
* Placa.
* Tipo de vehículo.
* Fecha de ingreso.
* Hora de ingreso.
* Empleado que realizó el registro.
* Estado.
* Observaciones opcionales.

Estados sugeridos:

* EN_PARQUEADERO
* FINALIZADO
* ANULADO

La placa debe normalizarse automáticamente:

* eliminar espacios innecesarios;
* convertir a mayúsculas;
* validar formato razonable.

No permitir normalmente dos parqueos activos simultáneos con la misma placa.

---

# 6. REGISTRO DE INGRESO

El registro debe ser extremadamente rápido.

Flujo esperado:

1. empleado selecciona "Nuevo ingreso";
2. escribe placa;
3. selecciona tipo de vehículo;
4. confirma;
5. sistema registra fecha y hora automáticamente;
6. sistema confirma visualmente el ingreso.

La fecha/hora debe ser asignada por el sistema.

La interfaz debe facilitar el uso frecuente desde dispositivos táctiles.

Evaluar incorporar:

* botones grandes;
* teclado optimizado;
* autocompletado;
* confirmación visual;
* sonido opcional;
* búsqueda inmediata de placa.

---

# 7. VEHÍCULOS EN PARQUEADERO

Crear una pantalla principal con los vehículos actualmente estacionados.

Mostrar:

* placa;
* tipo;
* hora de ingreso;
* tiempo transcurrido;
* empleado que realizó el ingreso.

Permitir:

* búsqueda por placa;
* filtrado;
* ordenamiento;
* selección inmediata para registrar salida.

La placa debe ser el principal elemento visual.

---

# 8. REGISTRO DE SALIDA

Cuando un vehículo salga:

1. buscar placa;
2. seleccionar parqueo activo;
3. registrar automáticamente fecha/hora de salida;
4. calcular tiempo estacionado;
5. aplicar regla tarifaria correspondiente;
6. mostrar claramente el valor;
7. seleccionar medio de pago;
8. confirmar cobro;
9. registrar transacción;
10. cerrar parqueo.

No permitir finalizar un parqueo sin registrar el cobro, salvo operación administrativa expresamente autorizada.

---

# 9. SISTEMA DE TARIFAS

El administrador debe poder configurar tarifas.

Como mínimo deben existir:

## Tarifa plena AM

Aplicable según las condiciones horarias definidas por el administrador.

## Tarifa plena PM

Aplicable según las condiciones horarias definidas por el administrador.

## Tarifa todo el día

Valor fijo para permanencias que cumplan la regla configurada.

## Tarifa por tiempo

Cobro según tiempo de permanencia.

La solución NO debe codificar valores tarifarios directamente en el código fuente.

Las tarifas deben almacenarse en base de datos.

Cada tarifa debería permitir configurar:

* nombre;
* descripción;
* tipo de vehículo;
* modalidad;
* valor;
* hora inicial;
* hora final;
* tolerancia;
* tiempo mínimo;
* unidad de tiempo;
* fracción;
* redondeo;
* prioridad;
* estado activo/inactivo;
* fecha de vigencia.

Diseñar un **motor de tarifas desacoplado**, utilizando cuando sea conveniente patrones como:

* Strategy;
* Factory;
* Chain of Responsibility.

La finalidad es poder agregar posteriormente:

* tarifa nocturna;
* tarifa especial;
* convenios;
* tarifa mensual;
* clientes frecuentes;
* vehículos exentos;
* tarifa fin de semana;
* tarifa festivos.

---

# 10. REGLAS DE CÁLCULO

La especificación debe definir exactamente:

* cuándo aplica AM;
* cuándo aplica PM;
* cuándo aplica todo el día;
* cuándo aplica tarifa por tiempo;
* cómo manejar fracciones;
* cómo realizar redondeos;
* qué ocurre cuando el parqueo cruza AM y PM;
* qué ocurre cuando cruza medianoche;
* qué ocurre durante más de 24 horas;
* qué tarifa tiene prioridad cuando varias reglas coinciden.

Ninguna regla ambigua debe implementarse sin documentarla.

Cuando exista ambigüedad funcional, debes:

1. identificarla;
2. documentarla como decisión pendiente;
3. proponer alternativas;
4. recomendar una implementación predeterminada razonable;
5. mantener la regla parametrizable cuando sea posible.

---

# 11. GESTIÓN DE PAGOS

Registrar:

* parqueo;
* valor calculado;
* valor cobrado;
* medio de pago;
* fecha;
* hora;
* usuario;
* número de operación.

Medios iniciales:

* efectivo;
* transferencia;
* otros.

Deben ser parametrizables posteriormente.

No almacenar información bancaria sensible.

Permitir generar comprobante de pago.

El comprobante puede incluir:

* nombre del parqueadero;
* NIT si está configurado;
* placa;
* tipo de vehículo;
* ingreso;
* salida;
* duración;
* tarifa aplicada;
* valor pagado;
* medio de pago;
* consecutivo;
* empleado.

Diseñar el comprobante para impresión y visualización digital.

---

# 12. CAJA

Incorporar un módulo básico de caja.

## Apertura

Registrar:

* empleado;
* fecha;
* hora;
* saldo inicial.

## Durante el turno

Registrar todos los pagos asociados a la caja activa.

## Cierre

Mostrar:

* saldo inicial;
* cobros en efectivo;
* cobros electrónicos;
* otros medios;
* total recaudado;
* cantidad de vehículos atendidos;
* efectivo esperado;
* efectivo reportado;
* diferencia.

Guardar cierre de caja histórico.

No permitir modificar un cierre finalizado sin intervención administrativa y registro de auditoría.

---

# 13. REPORTES

Implementar:

### Reporte diario

### Reporte semanal

### Reporte mensual

### Reporte por rango personalizado

Debe ser posible seleccionar:

* fecha inicial;
* fecha final.

Indicadores sugeridos:

* número de vehículos ingresados;
* número de vehículos finalizados;
* automóviles;
* motocicletas;
* total recaudado;
* recaudación por medio de pago;
* recaudación por tarifa;
* duración promedio;
* horas de mayor movimiento;
* ingresos por empleado;
* parqueos anulados;
* diferencias de caja.

Permitir posteriormente exportar:

* PDF;
* Excel;
* CSV.

No es obligatorio implementar las tres exportaciones en la primera versión, pero la arquitectura debe soportarlas.

---

# 14. DASHBOARD

El dashboard debe mostrar prioritariamente:

* vehículos actualmente estacionados;
* espacios ocupados;
* ingresos del día;
* salidas del día;
* recaudo del día;
* estado de caja;
* últimos movimientos.

Si se configura capacidad máxima, mostrar:

**Ocupados / Capacidad total**

y porcentaje de ocupación.

No sobrecargar visualmente el dashboard.

---

# 15. AUDITORÍA

Las operaciones importantes deben registrar:

* usuario;
* acción;
* fecha;
* hora;
* entidad afectada;
* identificador;
* valor anterior cuando corresponda;
* valor nuevo cuando corresponda.

Auditar especialmente:

* cambios de tarifa;
* anulaciones;
* modificaciones administrativas;
* reapertura de operaciones;
* cierres de caja;
* modificaciones de usuario.

Evitar borrar información transaccional físicamente.

Preferir:

* estados;
* anulaciones;
* soft delete;

cuando exista necesidad de conservar trazabilidad.

---

# 16. PERSISTENCIA

## Primera versión

Utilizar SQLite.

IMPORTANTE:

Aunque SQLite pueda almacenarse localmente, la aplicación debe persistir la información entre reinicios.

No confundir SQLite con almacenamiento temporal puramente en memoria.

Si se utiliza inicialmente SQLite en memoria exclusivamente durante desarrollo o pruebas:

`jdbc:sqlite::memory:`

debe existir también un perfil persistente usando archivo, por ejemplo:

`parking.db`

para operación real.

---

# 17. MIGRACIÓN FUTURA A MYSQL

Diseñar desde el principio la aplicación para migrar posteriormente hacia MySQL.

Utilizar abstracción de persistencia mediante:

* Repository;
* ORM cuando corresponda;
* separación dominio/infraestructura;
* migraciones de esquema.

Evitar:

* SQL específico de SQLite en lógica de negocio;
* tipos exclusivos de SQLite;
* dependencias innecesarias entre dominio y motor de base de datos.

Utilizar una herramienta de migración como:

* Flyway;

o equivalente.

Crear perfiles:

* development;
* test;
* production.

Ejemplo conceptual:

development → SQLite
production → MySQL

La migración futura debe requerir principalmente:

* configuración de datasource;
* driver;
* migraciones compatibles;

y no reescritura de la lógica funcional.

---

# 18. ARQUITECTURA

Utilizar una arquitectura limpia y modular.

Preferentemente:

**UI / Presentation
↓
Application
↓
Domain
↓
Infrastructure**

Aplicar:

* SOLID;
* Clean Architecture cuando resulte útil;
* separación de responsabilidades;
* Dependency Injection;
* Repository Pattern;
* Service Layer;
* DTO cuando corresponda.

Evitar sobreingeniería innecesaria para un sistema pequeño.

La solución debe ser sencilla de mantener y evolucionar.

---

# 19. PWA

La aplicación debe cumplir requisitos PWA.

Incluir:

* manifest;
* service worker;
* iconos;
* instalación;
* responsive design;
* standalone mode;
* pantalla de instalación cuando corresponda;
* estrategia de cache apropiada.

Debe funcionar correctamente en:

* computador;
* tablet;
* smartphone Android;
* smartphone iOS dentro de las limitaciones de PWA.

La aplicación debe poder abrirse y conservar su interfaz básica cuando exista pérdida temporal de conectividad.

IMPORTANTE:

No implementar sincronización offline compleja en la primera versión salvo que se especifique.

Separar claramente:

**offline de recursos estáticos**

de

**offline transaccional de datos**.

La arquitectura deberá permitir implementar sincronización de datos posteriormente.

---

# 20. UX/UI

Objetivo principal:

**velocidad + claridad + mínimo número de acciones.**

Aplicar:

* diseño mobile-first;
* responsive design;
* navegación clara;
* botones táctiles suficientemente grandes;
* contraste adecuado;
* tipografía legible;
* formularios cortos;
* mensajes comprensibles;
* prevención de errores;
* confirmación de operaciones críticas.

Evitar:

* interfaces saturadas;
* formularios innecesariamente extensos;
* exceso de ventanas modales;
* tablas imposibles de utilizar desde móviles.

---

# 21. NAVEGACIÓN PROPUESTA

Para EMPLEADO:

* Inicio
* Nuevo ingreso
* Parqueados
* Cobros
* Caja

Para ADMINISTRADOR:

* Dashboard
* Parqueados
* Historial
* Reportes
* Tarifas
* Usuarios
* Caja
* Configuración
* Auditoría

Diseña una navegación adaptada a escritorio y móvil.

En móvil considerar barra inferior para las funciones más utilizadas.

---

# 22. SEGURIDAD

Implementar:

* autenticación;
* autorización;
* contraseñas hasheadas;
* protección de rutas;
* RBAC;
* validaciones servidor;
* validaciones cliente;
* manejo seguro de sesiones o tokens;
* protección contra entradas inválidas;
* manejo centralizado de excepciones.

Nunca guardar contraseñas en texto plano.

---

# 23. CONCURRENCIA Y CONSISTENCIA

Evitar operaciones duplicadas.

Ejemplos:

* dos empleados intentando cobrar simultáneamente la misma placa;
* registrar dos salidas;
* dos ingresos activos para la misma placa;
* doble envío por pulsación repetida.

Implementar controles transaccionales e idempotencia cuando resulte necesario.

---

# 24. MODELO DE DOMINIO PRELIMINAR

Analiza y mejora las siguientes entidades:

User
Role
VehicleType
ParkingSession
Tariff
TariffRule
Payment
CashSession
CashMovement
Configuration
AuditLog

Considera relaciones, restricciones, índices y campos de auditoría.

Utilizar identificadores independientes del motor de base de datos.

---

# 25. FECHAS Y HORAS

Tratar correctamente las fechas.

Almacenar timestamps de forma consistente.

La zona horaria debe ser configurable.

Inicialmente considerar:

**America/Bogota**

No dispersar lógica de fecha y hora en todo el sistema.

Crear un servicio de tiempo o abstracción equivalente que facilite pruebas.

---

# 26. CONFIGURACIÓN DEL PARQUEADERO

Permitir configurar:

* nombre;
* NIT opcional;
* dirección;
* teléfono;
* capacidad máxima;
* zona horaria;
* moneda;
* formato de comprobante;
* tipos de vehículo;
* medios de pago;
* parámetros tarifarios.

Moneda inicial:

**COP — Peso colombiano**

No codificar permanentemente estos valores en la lógica.

---

# 27. STACK TECNOLÓGICO

Antes de comenzar la implementación:

1. propón un stack tecnológico;
2. explica brevemente sus ventajas;
3. selecciona tecnologías maduras y ampliamente soportadas;
4. evita dependencias innecesarias.

Si no existe otra instrucción, utiliza como arquitectura de referencia:

### Backend

Java + Spring Boot

### API

REST

### Persistencia

Spring Data JPA

### Desarrollo

SQLite

### Producción futura

MySQL

### Migraciones

Flyway

### Frontend

React + TypeScript + Vite

### PWA

Vite PWA / Workbox

### UI

Material UI, Tailwind CSS o equivalente.

Selecciona UNA estrategia UI principal y mantenla consistente.

### Pruebas Backend

JUnit + Mockito

### Pruebas Frontend

Vitest + Testing Library

### E2E

Playwright

### Contenedores

Docker + Docker Compose

---

# 28. ESPECIFICACIONES COMO ARTEFACTOS

Crear dentro del proyecto:

`/specs`

con documentos como:

```text
/specs
   00-product-vision.md
   01-functional-requirements.md
   02-non-functional-requirements.md
   03-domain-model.md
   04-use-cases.md
   05-business-rules.md
   06-architecture.md
   07-data-model.md
   08-api-contract.md
   09-ux-specification.md
   10-security-specification.md
   11-test-strategy.md
   /features
```

Cada funcionalidad tendrá su propia especificación:

```text
/specs/features
   parking-entry.md
   parking-exit.md
   tariff-calculation.md
   payments.md
   cash-register.md
   reports.md
```

---

# 29. FORMATO OBLIGATORIO PARA CADA FEATURE

Para cada funcionalidad generar:

## FEATURE-ID

Ejemplo:

`PARK-ENTRY-001`

## Nombre

Registro de ingreso

## Objetivo

## Actor

## Precondiciones

## Flujo principal

## Flujos alternativos

## Excepciones

## Reglas de negocio

## Datos de entrada

## Datos de salida

## Validaciones

## Criterios de aceptación

En formato:

**Given / When / Then**

Ejemplo:

Given que no existe un parqueo activo para la placa ABC123
When el empleado registra el ingreso
Then el sistema crea una sesión de parqueo activa.

## Casos límite

## Impacto sobre datos

## Endpoints relacionados

## Pantallas relacionadas

## Pruebas unitarias

## Pruebas de integración

## Pruebas E2E

## Definition of Done

Solo cuando esta especificación esté completa se debe generar el código.

---

# 30. TRAZABILIDAD

Mantener una matriz de trazabilidad:

| Requisito | Feature | Caso de uso | API | Componente | Prueba |
| --------- | ------- | ----------- | --- | ---------- | ------ |

Ejemplo:

RF-001
→ PARK-ENTRY-001
→ UC-001
→ POST /parking-sessions
→ ParkingEntryService
→ ParkingEntryServiceTest

No debe existir funcionalidad significativa sin requisito asociado.

No debe existir requisito implementado sin pruebas asociadas.

---

# 31. API

Diseñar una API REST consistente.

Ejemplos conceptuales:

```text
POST /api/auth/login

GET /api/parking-sessions/active
POST /api/parking-sessions
GET /api/parking-sessions/{id}
POST /api/parking-sessions/{id}/checkout

GET /api/tariffs
POST /api/tariffs
PUT /api/tariffs/{id}

POST /api/payments

POST /api/cash-sessions/open
POST /api/cash-sessions/{id}/close

GET /api/reports/daily
GET /api/reports/summary

GET /api/users
POST /api/users
```

Generar contrato OpenAPI.

La especificación de API debe formar parte de SDD.

---

# 32. PRUEBAS

Utilizar pirámide de pruebas.

## Unitarias

Especialmente:

* motor tarifario;
* servicios de dominio;
* validaciones.

## Integración

* repositorios;
* API;
* autenticación;
* transacciones.

## E2E

Flujos prioritarios:

### Flujo 1

Login → registrar ingreso → comprobar vehículo activo.

### Flujo 2

Buscar placa → calcular tarifa → pagar → registrar salida.

### Flujo 3

Abrir caja → operaciones → cerrar caja.

### Flujo 4

Administrador → modificar tarifa → verificar nueva tarifa.

### Flujo 5

Generar reporte diario.

El motor tarifario debe disponer de una batería amplia de pruebas de borde.

---

# 33. DATOS DE DEMOSTRACIÓN

Crear datos seed para desarrollo.

Usuarios iniciales:

Administrador de prueba.

Empleado de prueba.

Nunca utilizar estas credenciales predeterminadas en producción.

Agregar tarifas ficticias configurables para pruebas.

---

# 34. DOCKER

Entregar:

* Dockerfile backend;
* Dockerfile frontend cuando corresponda;
* docker-compose.yml;
* variables mediante `.env`;
* `.env.example`.

Preparar Docker Compose para permitir posteriormente:

```text
frontend
backend
mysql
```

Durante el desarrollo con SQLite no debe ser obligatorio levantar MySQL.

---

# 35. OBSERVABILIDAD

Incorporar logging adecuado para:

* inicio de sesión;
* errores;
* operaciones críticas;
* cálculo de tarifas;
* cierre de caja.

No registrar:

* contraseñas;
* tokens;
* secretos.

Implementar health check de backend.

---

# 36. RESPALDO

Aunque inicialmente sea una aplicación pequeña, definir estrategia para:

* backup SQLite;
* restauración;
* futura copia MySQL.

La pérdida de la base de datos no debe implicar necesariamente pérdida irreversible de toda la operación.

---

# 37. DEFINITION OF DONE GLOBAL

Una feature estará terminada únicamente cuando:

* exista especificación;
* requisitos estén documentados;
* criterios de aceptación estén definidos;
* modelo de dominio esté actualizado;
* API esté documentada;
* implementación compile;
* pruebas unitarias pasen;
* pruebas de integración pasen;
* validaciones funcionen;
* manejo de errores esté implementado;
* interfaz sea responsive;
* control de permisos funcione;
* documentación se encuentre actualizada.

---

# 38. METODOLOGÍA DE CONSTRUCCIÓN POR FASES

Debes desarrollar el proyecto en las siguientes fases.

## FASE 0 — DESCUBRIMIENTO

Crear:

* visión del producto;
* alcance;
* actores;
* glosario;
* supuestos;
* restricciones;
* preguntas pendientes.

NO crear todavía código.

---

## FASE 1 — ESPECIFICACIÓN

Crear:

* requisitos funcionales;
* requisitos no funcionales;
* casos de uso;
* reglas de negocio;
* criterios de aceptación;
* matriz de trazabilidad.

NO implementar todavía funcionalidades.

---

## FASE 2 — MODELO Y ARQUITECTURA

Crear:

* modelo de dominio;
* diagrama de componentes;
* arquitectura lógica;
* modelo de datos;
* ERD;
* estrategia SQLite → MySQL;
* arquitectura PWA;
* estrategia de seguridad;
* contratos de API.

---

## FASE 3 — UX/UI

Diseñar:

* mapa de navegación;
* user flows;
* wireframes;
* design system;
* componentes reutilizables;
* diseño responsive.

Pantallas mínimas:

1. Login.
2. Dashboard.
3. Nuevo ingreso.
4. Vehículos parqueados.
5. Cobro/salida.
6. Caja.
7. Historial.
8. Reportes.
9. Tarifas.
10. Usuarios.
11. Configuración.

---

## FASE 4 — FOUNDATION

Implementar:

* estructura backend;
* estructura frontend;
* configuración;
* seguridad base;
* persistencia SQLite;
* migraciones;
* manejo de errores;
* logging;
* configuración PWA.

---

## FASE 5 — OPERACIÓN PRINCIPAL

Implementar:

* ingreso;
* vehículos activos;
* salida;
* cálculo tarifario;
* pagos.

---

## FASE 6 — CAJA

Implementar:

* apertura;
* movimientos;
* cierre;
* arqueo.

---

## FASE 7 — ADMINISTRACIÓN

Implementar:

* usuarios;
* tarifas;
* configuración;
* auditoría.

---

## FASE 8 — REPORTES

Implementar:

* diario;
* semanal;
* mensual;
* personalizado.

---

## FASE 9 — PWA Y UX

Validar:

* instalación;
* responsive;
* service worker;
* cache;
* experiencia móvil;
* accesibilidad básica;
* rendimiento.

---

## FASE 10 — QA

Ejecutar:

* pruebas unitarias;
* integración;
* E2E;
* pruebas tarifarias;
* pruebas de seguridad;
* pruebas responsive.

---

## FASE 11 — CONTENEDORIZACIÓN

Crear:

* Dockerfiles;
* docker-compose;
* variables de entorno;
* documentación de despliegue.

---

## FASE 12 — PREPARACIÓN MYSQL

Comprobar que el proyecto pueda cambiar de:

SQLite

a:

MySQL

sin modificar la lógica de dominio.

Documentar el procedimiento.

---

# 39. FORMA DE TRABAJO CONMIGO

Trabajaremos iterativamente.

NO avances automáticamente por todas las fases.

En cada iteración:

1. indica la fase actual;
2. identifica el objetivo;
3. genera los artefactos correspondientes;
4. indica decisiones tomadas;
5. identifica decisiones pendientes;
6. comprueba la coherencia con especificaciones anteriores;
7. espera la siguiente instrucción antes de continuar a otra fase.

Nunca modifiques silenciosamente un requisito previamente aprobado.

Cuando sea necesario cambiar una especificación:

1. identifica qué cambia;
2. explica qué otros artefactos afecta;
3. actualiza trazabilidad;
4. actualiza pruebas;
5. registra la decisión.

---

# 40. CONTROL DE CAMBIOS SDD

Crear:

`/specs/decisions`

para Architecture Decision Records:

```text
ADR-001-stack-tecnologico.md
ADR-002-persistencia-sqlite.md
ADR-003-motor-tarifario.md
ADR-004-autenticacion.md
```

Formato ADR:

* Contexto.
* Problema.
* Alternativas.
* Decisión.
* Justificación.
* Consecuencias.

---

# 41. PRINCIPIOS DE IMPLEMENTACIÓN

Priorizar en este orden:

1. Correctitud.
2. Integridad de la información.
3. Simplicidad.
4. Usabilidad.
5. Mantenibilidad.
6. Seguridad.
7. Rendimiento.
8. Extensibilidad.

No implementar complejidad innecesaria.

No generar código placeholder como implementación definitiva.

No dejar:

* TODO críticos;
* métodos vacíos;
* endpoints simulados;
* dependencias inexistentes;
* configuraciones incompletas.

Todo código entregado debe:

* compilar;
* ser coherente entre archivos;
* incluir imports;
* incluir dependencias necesarias;
* utilizar versiones compatibles;
* incluir instrucciones de ejecución.

---

# 42. PRIMERA TAREA

Comienza exclusivamente con:

# FASE 0 — DESCUBRIMIENTO Y VISIÓN DEL PRODUCTO

Entrégame:

1. visión del producto;
2. alcance de la versión MVP;
3. funcionalidades fuera del alcance inicial;
4. actores;
5. mapa general de módulos;
6. requisitos funcionales preliminares identificados;
7. requisitos no funcionales preliminares;
8. reglas de negocio que requieren definición;
9. riesgos técnicos;
10. riesgos funcionales;
11. glosario inicial;
12. propuesta preliminar de stack;
13. propuesta de arquitectura;
14. roadmap de las siguientes fases SDD.

Adicionalmente crea una tabla de decisiones pendientes, especialmente relacionada con:

* horarios AM y PM;
* cálculo de fracciones;
* tolerancia;
* reglas de todo el día;
* tarifas diferentes para carro/moto;
* vehículos que pasan la medianoche;
* permanencias superiores a 24 horas;
* capacidad máxima del parqueadero;
* impresión de comprobantes;
* medios de pago;
* manejo offline.

En esta primera respuesta:

**NO generes código todavía.**

El objetivo es obtener primero una especificación sólida y verificable sobre la cual construir el sistema.
