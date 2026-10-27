# 01 — Requisitos Funcionales

> Fase 1 — SDD. Formaliza y amplía los requisitos preliminares de `00-product-vision.md` §6. Prioridad en escala MoSCoW: **M**ust, **S**hould, **C**ould. Cada requisito referencia las reglas de negocio (`05-business-rules.md`) que lo restringen, cuando aplica.

## Módulo: Autenticación

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-001 | El sistema debe permitir iniciar sesión con usuario y contraseña. | Administrador, Empleado | M | BR-004 |
| RF-002 | El sistema debe rechazar credenciales inválidas sin revelar si el usuario existe o no. | — | M | BR-004 |
| RF-003 | El sistema debe cerrar sesión invalidando el token/sesión activa. | Administrador, Empleado | M | — |
| RF-004 | El sistema debe bloquear el acceso a rutas/endpoints protegidos a usuarios no autenticados. | — | M | — |

## Módulo: Usuarios y roles

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-005 | El administrador debe poder crear usuarios asignando rol (Administrador/Empleado). | Administrador | M | BR-003 |
| RF-006 | El administrador debe poder activar/desactivar usuarios. Un usuario desactivado no puede iniciar sesión. | Administrador | M | — |
| RF-007 | El administrador debe poder cambiar la contraseña de cualquier usuario. | Administrador | M | BR-004 |
| RF-008 | Un usuario debe poder cambiar su propia contraseña. | Administrador, Empleado | S | BR-004 |
| RF-009 | El sistema debe impedir que un Empleado acceda a funciones de gestión de usuarios. | — | M | BR-003 |

## Módulo: Configuración del parqueadero

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-010 | El administrador debe poder configurar nombre, NIT (opcional), dirección, teléfono del parqueadero. | Administrador | M | — |
| RF-011 | El administrador debe poder configurar la capacidad máxima de cupos. | Administrador | M | BR-016 |
| RF-012 | El administrador debe poder configurar la zona horaria del parqueadero (semilla: `America/Bogota`). | Administrador | M | BR-026 |
| RF-013 | El administrador debe poder configurar la moneda (semilla: COP). | Administrador | S | — |
| RF-014 | El administrador debe poder configurar el catálogo de medios de pago. | Administrador | M | BR-018 |
| RF-015 | El administrador debe poder configurar los campos visibles del comprobante de pago y si se ofrece imprimir comprobante al ingreso (`printOnEntry`, por defecto no) y/o a la salida (`printOnExit`, por defecto sí). | Administrador | S | BR-017 |

## Módulo: Tipos de vehículo

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-016 | El sistema debe soportar como mínimo los tipos "automóvil" y "motocicleta" desde datos semilla. | — | M | — |
| RF-017 | El administrador debe poder agregar nuevos tipos de vehículo sin cambios de código. | Administrador | S | — |

## Módulo: Tarifas

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-018 | El administrador debe poder crear una tarifa indicando: nombre, descripción, tipo de vehículo, modalidad, valor, hora inicial, hora final, tolerancia, tiempo mínimo, unidad de tiempo, fracción, redondeo, prioridad, estado activo/inactivo, vigencia. | Administrador | M | BR-005, BR-006, BR-009 |
| RF-019 | El administrador debe poder editar una tarifa existente. | Administrador | M | BR-006 |
| RF-020 | El administrador debe poder activar/desactivar una tarifa. | Administrador | M | BR-009 |
| RF-021 | El sistema debe impedir guardar una tarifa con datos incompletos o inconsistentes (ej. hora final antes que inicial en modalidades que lo requieran). | — | M | — |
| RF-022 | Toda modificación de tarifa debe quedar registrada en auditoría. | — | M | BR-025 |

## Módulo: Registro de ingreso

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-023 | El empleado debe poder registrar el ingreso de un vehículo indicando placa y tipo de vehículo. | Empleado | M | BR-001, BR-002 |
| RF-024 | El sistema debe normalizar la placa automáticamente (mayúsculas, sin espacios extra) antes de guardar. | — | M | BR-002 |
| RF-025 | El sistema debe asignar automáticamente fecha y hora de ingreso (no editable manualmente por el empleado). | — | M | BR-026 |
| RF-026 | El sistema debe impedir un segundo ingreso activo para una placa que ya tiene sesión `EN_PARQUEADERO`. | — | M | BR-001 |
| RF-027 | El sistema debe confirmar visualmente el ingreso registrado. | — | M | — |
| RF-028 | El sistema debe permitir registrar observaciones opcionales en el ingreso. | Empleado | C | — |
| RF-029 | El sistema debe registrar qué empleado realizó el ingreso. | — | M | — |
| RF-072 | El sistema debe permitir imprimir un comprobante de ingreso como soporte para el cliente, con consecutivo `ING-000001`, placa, tipo de vehículo, fecha/hora de ingreso, observaciones y (según el formato) el empleado que registró. Se ofrece tras el ingreso solo si está activado en la configuración (RF-015) y siempre se puede reimprimir desde Historial. | Empleado, Administrador | S | BR-027 |

## Módulo: Vehículos actualmente estacionados

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-030 | El sistema debe listar todas las sesiones en estado `EN_PARQUEADERO` con placa, tipo, hora de ingreso, tiempo transcurrido y empleado que ingresó. | Administrador, Empleado | M | — |
| RF-031 | El sistema debe permitir buscar por placa (búsqueda parcial/instantánea) dentro de los vehículos activos. | Administrador, Empleado | M | — |
| RF-032 | El sistema debe permitir filtrar y ordenar la lista de vehículos activos (ej. por tipo, por tiempo de permanencia). | Administrador, Empleado | S | — |
| RF-033 | El sistema debe permitir seleccionar un vehículo activo directamente desde esta pantalla para iniciar su salida. | Empleado | M | — |

## Módulo: Registro de salida

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-034 | El empleado debe poder buscar y seleccionar una sesión activa por placa para registrar su salida. | Empleado | M | — |
| RF-035 | El sistema debe asignar automáticamente fecha y hora de salida. | — | M | BR-026 |
| RF-036 | El sistema debe calcular el tiempo de permanencia entre ingreso y salida. | — | M | BR-014 |
| RF-037 | El sistema debe calcular automáticamente el valor a cobrar aplicando la tarifa correspondiente. | — | M | BR-005 a BR-016 |
| RF-038 | El sistema debe mostrar claramente el valor calculado antes de confirmar el cobro. | — | M | — |
| RF-039 | El empleado debe poder seleccionar el medio de pago. | Empleado | M | BR-018 |
| RF-040 | El sistema debe registrar la transacción de pago al confirmar el cobro. | — | M | BR-021 |
| RF-041 | El sistema debe cerrar la sesión de parqueo (`FINALIZADO`) solo después de registrar el pago. | — | M | BR-020 |
| RF-042 | El sistema no debe permitir doble cobro o doble cierre sobre la misma sesión. | — | M | BR-021 |

## Módulo: Motor de cálculo tarifario

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-043 | El motor debe seleccionar la modalidad de tarifa aplicable según tipo de vehículo, horario de ingreso/salida y reglas de prioridad. | — | M | BR-007, BR-008 |
| RF-044 | El motor debe aplicar tolerancia antes de calcular cualquier fracción cobrable. | — | M | BR-012 |
| RF-045 | El motor debe aplicar la unidad de fracción y política de redondeo configuradas en la tarifa. | — | M | BR-011 |
| RF-046 | El motor debe manejar correctamente permanencias que cruzan la medianoche. | — | M | BR-014 |
| RF-047 | El motor debe manejar correctamente permanencias superiores a 24 horas. | — | M | BR-015 |
| RF-048 | El motor debe ser extensible a nuevas modalidades de tarifa sin modificar las estrategias existentes. | — | S | BR-007 |

## Módulo: Gestión de cobros

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-049 | El sistema debe registrar por cada pago: sesión de parqueo, valor calculado, valor cobrado, medio de pago, fecha, hora, usuario, número de operación. | — | M | — |
| RF-050 | El sistema debe permitir generar un comprobante de pago imprimible/digital con los datos mínimos definidos en el prompt maestro §11. | — | M | BR-017 |
| RF-051 | El sistema no debe almacenar información bancaria sensible. | — | M | — |

## Módulo: Caja

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-052 | El empleado debe poder abrir caja indicando saldo inicial; el sistema registra empleado, fecha y hora automáticamente. | Empleado | M | BR-022 |
| RF-053 | El sistema debe impedir abrir una nueva caja si el empleado ya tiene una caja abierta. | — | M | BR-022 |
| RF-054 | El sistema debe asociar automáticamente cada pago a la caja activa del empleado que lo registra. | — | M | BR-022 |
| RF-055 | El empleado debe poder cerrar caja, y el sistema debe mostrar: saldo inicial, cobros en efectivo, cobros electrónicos, otros medios, total recaudado, cantidad de vehículos atendidos, efectivo esperado, efectivo reportado, diferencia. | Empleado | M | — |
| RF-056 | El sistema debe guardar cada cierre de caja como registro histórico inmutable. | — | M | BR-023 |
| RF-057 | El sistema no debe permitir modificar un cierre de caja finalizado sin intervención administrativa auditada. | — | M | BR-023 |
| RF-079 | El administrador debe poder mantener un catálogo de conceptos de otros ingresos y de gastos de caja (crear, renombrar, activar/desactivar); el nombre es único por tipo. | Administrador | S | BR-032 |
| RF-080 | Cualquier usuario con caja abierta debe poder registrar en ella otro ingreso (concepto activo, descripción, valor y medio de pago). | Empleado, Administrador | S | BR-032, BR-033, BR-034 |
| RF-081 | El administrador debe poder registrar un gasto o salida de caja en cualquier caja abierta (la suya o la de un empleado); un gasto en efectivo no puede superar el efectivo disponible. | Administrador | S | BR-032, BR-033, BR-034 |
| RF-082 | Cada otro ingreso o gasto debe tener un comprobante imprimible con su número OP-, visible para el administrador, quien lo registró y el dueño de la caja. | Empleado, Administrador | S | BR-034, BR-017 |
| RF-083 | El administrador debe poder anular un otro ingreso o gasto, con motivo, mientras la caja sigue abierta; y consultar y exportar (Excel, PDF, CSV) un reporte de ingresos y gastos por período, tipo, concepto y estado. El reporte general incluye otros ingresos, gastos y neto. | Administrador | S | BR-035 |
| RF-084 | El Dashboard debe mostrar los otros ingresos y los gastos de caja del día; los otros ingresos suman en el recaudo de hoy. | Administrador | S | BR-033 |

## Módulo: Historial de operaciones

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-058 | El sistema debe permitir consultar el historial de sesiones de parqueo (activas, finalizadas, anuladas) con filtros por fecha, placa, estado y empleado. | Administrador | M | — |
| RF-059 | El empleado debe poder consultar únicamente las operaciones de su propio turno/caja activa. | Empleado | M | BR-003 |

## Módulo: Reportes

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-060 | El sistema debe generar un reporte diario con los indicadores mínimos del prompt maestro §13. | Administrador | M | — |
| RF-061 | El sistema debe generar reportes semanal y mensual con los mismos indicadores. | Administrador | S | — |
| RF-062 | El sistema debe permitir un reporte por rango de fechas personalizado (fecha inicial y final). | Administrador | M | — |
| RF-063 | La arquitectura de reportes debe soportar exportación a PDF, Excel y CSV. **Excel y PDF implementados** (2026-09-29, a pedido del usuario; ver `decisions/ADR-008-exportacion-reportes.md`); CSV queda como un exportador más, sin cambios de arquitectura. | Administrador | C | — |

## Módulo: Dashboard

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-064 | El dashboard debe mostrar: vehículos actualmente estacionados, espacios ocupados, ingresos del día, salidas del día, recaudo del día, estado de caja, últimos movimientos. | Administrador | M | — |
| RF-065 | Si hay capacidad máxima configurada, el dashboard debe mostrar "Ocupados/Capacidad total" y porcentaje de ocupación. | — | M | BR-016 |

## Módulo: Auditoría

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-066 | El sistema debe registrar auditoría de: cambios de tarifa, anulaciones, modificaciones administrativas, reapertura de operaciones, cierres de caja, modificaciones de usuario. | — | M | BR-025 |
| RF-067 | El administrador debe poder consultar el log de auditoría con filtros por usuario, entidad, fecha. | Administrador | M | — |
| RF-068 | El administrador debe poder anular un registro de parqueo dejando trazabilidad (motivo, usuario, fecha/hora). | Administrador | M | BR-020, BR-024, BR-025 |

## Módulo: Mensualidades

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-074 | El administrador debe poder crear, editar y activar/desactivar mensualidades: cliente, documento, teléfono, valor mensual pactado, día de corte (1–28), notas y una o varias placas autorizadas (cada placa pertenece a una sola mensualidad). Empleados y administradores pueden consultarlas con su estado. | Administrador | S | BR-028, BR-025 |
| RF-075 | Al registrar el ingreso de una placa autorizada, el sistema debe reconocerla como cliente mensual; si la mensualidad ya tiene otro vehículo dentro, el ingreso es de cliente normal y se avisa. | Empleado, Administrador | S | BR-029 |
| RF-076 | La salida de un cliente mensual al día o en días de gracia se registra sin cobro ni medio de pago; vencida, sin pago o inactiva, se cobra con las tarifas normales y la pantalla lo advierte. | Empleado, Administrador | S | BR-030, BR-020 |
| RF-077 | El pago mensual se cobra en la caja abierta con un medio de pago, por el valor pactado; extiende la vigencia hasta el siguiente corte, cuenta en el arqueo y en los reportes, recibe consecutivo `OP-` y genera comprobante imprimible. | Empleado, Administrador | S | BR-031 |
| RF-078 | El administrador debe poder consultar y exportar (Excel, PDF, CSV) un reporte de mensualidades con: estado de cartera (cliente, placas, valor, corte, pagada hasta, estado y días de mora a hoy, pagado en el período), detalle de pagos del período (fecha, OP-, período cubierto, valor, medio de pago, quién cobró) e indicadores (recaudo, conteo por estado, cartera vencida estimada); filtrable por rango de fechas, estados (varios) y cliente/documento/placa. | Administrador | S | BR-028, BR-031 |

## Módulo: PWA / Plataforma

| ID | Descripción | Actor | Prioridad | Reglas relacionadas |
|---|---|---|---|---|
| RF-069 | La aplicación debe poder instalarse como PWA (manifest + iconos + service worker). | — | M | BR-019 |
| RF-070 | La aplicación debe mostrar el shell básico de la interfaz aun con pérdida temporal de conectividad. | — | M | BR-019 |
| RF-071 | El backend debe exponer un endpoint de health check. | — | M | — |
| RF-073 | El administrador debe poder exportar la base de datos completa a un archivo JSON portable (SQLite o MySQL) e importarlo en otra ejecución, reemplazando todos los datos de forma atómica, solo si la versión del esquema coincide y el respaldo tiene al menos un administrador activo. Ambas acciones quedan auditadas. | Administrador | S | BR-025 |
