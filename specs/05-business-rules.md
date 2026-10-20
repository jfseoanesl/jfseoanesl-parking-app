# 05 — Reglas de Negocio

> Fase 1 — SDD. Este documento formaliza y **cierra** las decisiones que en `00-product-vision.md` (sección 15) quedaron marcadas como "pendiente de confirmación". El usuario las confirmó el 2026-09-22 usando la recomendación por defecto en cada caso. A partir de aquí son reglas vinculantes para el diseño del motor de tarifas y de las demás features.

Convención: cada regla indica si es **parametrizable** (configurable por el Administrador sin cambio de código) y su **valor semilla** por defecto.

## Identidad y acceso

**BR-001 — Unicidad de sesión activa por placa**
No puede existir más de una sesión de parqueo en estado `EN_PARQUEADERO` para la misma placa al mismo tiempo.
*Parametrizable:* no. *Origen:* prompt maestro §5.

**BR-002 — Normalización de placa**
Toda placa se normaliza antes de guardar: se eliminan espacios redundantes y se convierte a mayúsculas. Se valida contra un formato razonable (alfanumérico, longitud típica de placas colombianas 6-7 caracteres); el formato exacto es configurable para tolerar variantes (motos, remolques, placas especiales).
*Parametrizable:* el patrón de validación, sí. *Origen:* prompt maestro §5.

**BR-003 — RBAC estricto**
Un usuario con rol Empleado nunca puede ejecutar operaciones reservadas a Administrador (gestión de usuarios, tarifas, configuración, auditoría, anulaciones), ni siquiera invocando la API directamente. La validación de permisos ocurre siempre en el servidor; el cliente solo oculta opciones como ayuda de UX.
*Parametrizable:* no (los permisos por rol sí son datos, pero la aplicación de la regla no es opcional). *Origen:* prompt maestro §3, §22.

**BR-004 — Contraseñas**
Las contraseñas se almacenan siempre con hash (nunca texto plano ni cifrado reversible). El algoritmo de hash es responsabilidad de la capa de infraestructura y se define en `10-security-specification.md`.
*Parametrizable:* no. *Origen:* prompt maestro §22.

## Tarifas — reglas cerradas en esta iteración

**BR-005 — Tarifas independientes por tipo de vehículo**
Cada tarifa se asocia a un único tipo de vehículo (auto o moto). No existen tarifas "genéricas" aplicables a cualquier tipo; si un tipo de vehículo no tiene tarifa configurada para una modalidad, esa modalidad no está disponible para ese tipo.
*Parametrizable:* sí (el administrador crea/edita tarifas por tipo). *Decisión confirmada por el usuario.*

**BR-006 — No hardcodear valores tarifarios**
Ningún valor de tarifa, horario, tolerancia o fracción se codifica en el código fuente. Todo vive en la tabla de tarifas y se lee en tiempo de ejecución.
*Parametrizable:* sí, por definición. *Origen:* prompt maestro §9.

**BR-007 — Motor de tarifas desacoplado**
El cálculo se resuelve mediante una estrategia por modalidad de tarifa (AM, PM, todo el día, por tiempo), seleccionada por una fábrica según tipo de vehículo y condiciones de la sesión, con resolución de prioridad mediante cadena de responsabilidad cuando varias tarifas activas coinciden para la misma combinación tipo de vehículo + horario.
*Parametrizable:* la prioridad entre tarifas es un campo configurable (`prioridad`) de cada tarifa. *Origen:* prompt maestro §9.

**BR-008 — Prioridad entre tarifas coincidentes**
Cuando más de una tarifa activa aplica a la misma sesión (mismo tipo de vehículo, condiciones horarias superpuestas), se selecciona la de menor valor numérico en el campo `prioridad` (convención: 1 = mayor prioridad). Si dos tarifas empatan en prioridad, se selecciona la que produzca el **menor cobro** para el cliente (regla conservadora por defecto) y se registra en auditoría cuál se aplicó y por qué.
*Parametrizable:* el campo prioridad, sí. *Origen:* prompt maestro §10 (regla antes ambigua, ahora cerrada).

**BR-008.1 — Tarifa alterna elegida al cobrar**
La tarifa resuelta por BR-008 es una **sugerencia**. Al registrar el cobro (UC-004), el usuario que cobra (Empleado o Administrador) puede elegir otra de la lista de **tarifas alternas**: únicamente las tarifas activas, vigentes y **aplicables** a la sesión (mismo tipo de vehículo y franja horaria que aplica según BR-010 / BR-013 / BR-014), ya calculadas con sus propias reglas de fracción, tolerancia y redondeo (BR-011, BR-012), ordenadas por BR-008 con la sugerida primero.
- El valor lo calcula siempre el motor con la tarifa elegida; el cliente solo indica cuál. Una tarifa que no sea aplicable a la sesión se rechaza (`TARIFF_NOT_APPLICABLE`).
- El valor cobrado enviado al confirmar debe ser **exactamente** el calculado por el motor en ese momento (RF-037); si difiere (monto inventado, o cotización desactualizada porque pasó una fracción de tiempo) el cobro se rechaza con `CHARGED_AMOUNT_MISMATCH` sin registrar nada, y la pantalla vuelve a cotizar y pide confirmar el valor nuevo. Nunca se cobra un valor que no se le mostró al usuario.
- La sección "Tarifa" de la pantalla de cobro se muestra **siempre** (confirmado por el usuario, 2026-09-29): con varias aplicables permite elegir; con una sola la muestra fija con una nota que explica por qué no hay alternativa (única aplicable en ese horario, o permanencia mayor a 24 h, BR-015).
- No se exige motivo. Si la elegida difiere de la sugerida, se registra en auditoría (BR-025) la acción `TARIFF_OVERRIDDEN` con la tarifa sugerida y su valor (valor anterior) y la elegida y su valor (valor nuevo).
- Permanencias mayores a 24 h (BR-015) no ofrecen alternas: se mantiene el cálculo compuesto (días "todo el día" + remanente), que no corresponde a una sola tarifa.
*Parametrizable:* no (el conjunto de alternas se deriva de las tarifas configuradas). *Origen:* solicitud del usuario (2026-09-28); alcance, permisos y ausencia de motivo confirmados por el usuario.

**BR-009 — Vigencia de tarifas**
Una tarifa solo aplica si está `activa` y la fecha de la sesión está dentro de su rango de vigencia (`fecha_inicio_vigencia` / `fecha_fin_vigencia`, este último opcional = indefinida).
*Parametrizable:* sí. *Origen:* prompt maestro §9 (campo "fecha de vigencia").

**BR-010 — Horarios AM/PM configurables**
Los rangos horarios que definen "tarifa plena AM" y "tarifa plena PM" son configurables por tarifa (cada tarifa AM/PM define su propia `hora_inicial` y `hora_final`). Valor semilla sugerido: AM = 06:00–18:00, PM = 18:00–06:00. No se asume un único horario global del sistema; cada tarifa puede tener su propia franja.
*Parametrizable:* sí (por tarifa). *Decisión confirmada por el usuario.*

**BR-011 — Cálculo de fracciones**
El tiempo de permanencia se cobra en bloques de `fraccion_minutos` (valor de `value` = precio por bloque). Toda fracción iniciada, por mínima que sea, se cobra como fracción completa (redondeo hacia arriba / "ceiling") salvo que la tarifa configure otra política en `redondeo`, y salvo que la sesión esté dentro de la tolerancia (BR-012).
*Parametrizable:* la duración de la fracción (`fraccion_minutos`) y la política de redondeo (`UP`/`DOWN`/`NEAREST`, default `UP`).
*Nota de implementación (Fase 5):* el campo `unidad_tiempo` (`MINUTES`/`HOURS`) es metadato informativo para la UI de administración de tarifas (Fase 7) — indica en qué unidad piensa el administrador la fracción al configurarla — pero el motor de cálculo siempre opera internamente en minutos usando `fraccion_minutos` como valor ya convertido, para no duplicar lógica de conversión en el motor.
*Decisión confirmada por el usuario.*

**BR-012 — Tolerancia y tiempo mínimo**
Cada tarifa puede definir una `tolerancia` en minutos (valor semilla: 10 minutos) y opcionalmente un `tiempo_minimo` en minutos.
- Si el tiempo total de permanencia es menor o igual a la tolerancia, el cobro es $0 (no se factura ninguna fracción).
- Si el tiempo total de permanencia supera la tolerancia, se factura sobre el **tiempo total** de permanencia (la tolerancia es un umbral de gracia, no un descuento que se resta del tiempo facturable — comportamiento estándar de parquímetro: "primeros N minutos gratis, luego se cobra el periodo completo").
- Si `tiempo_minimo` está configurado y el tiempo facturable resultante es menor a `tiempo_minimo`, se factura como si hubiera durado `tiempo_minimo` (piso mínimo de cobro).
*Parametrizable:* sí, ambos campos por tarifa. *Decisión confirmada por el usuario; algoritmo precisado el 2026-09-22 (Fase 5).*

**BR-013 — Activación de tarifa "todo el día"**
Una tarifa "todo el día" aplica cuando el ingreso de la sesión ocurre dentro de la franja horaria configurada para esa tarifa (`hora_inicial`/`hora_final` de la tarifa "todo el día").

*Mecanismo concreto de la "comparación automática de costo" (cierra la ambigüedad dejada abierta en la versión anterior de esta regla, sin requerir un campo nuevo en el esquema):* la comparación de costo NO es un flag aparte — es una consecuencia directa de BR-008. Si el administrador quiere que "todo el día" jamás cobre más que la tarifa por tiempo equivalente, configura ambas tarifas (FULL_DAY y BY_TIME) del mismo tipo de vehículo con **la misma prioridad**: el motor calculará ambas y aplicará la de menor valor (BR-008). Si en cambio quiere que "todo el día" domine siempre que su franja horaria aplique (sin comparar costo), le asigna una prioridad estrictamente menor (más alta) que la tarifa por tiempo.
*Parametrizable:* la franja horaria y la prioridad relativa entre tarifas (que determina si hay comparación de costo). *Decisión confirmada por el usuario; mecanismo de implementación precisado el 2026-09-22 (Fase 5).*

**BR-014 — Cruce de medianoche**
El cálculo de tiempo de permanencia es continuo: no se “corta” ni se recalcula por cruzar la medianoche o cambiar de día calendario. La franja horaria (AM/PM) que aplica es la vigente en el **momento de la salida**, salvo que la tarifa "todo el día" ya se haya activado por la franja de ingreso (BR-013), en cuyo caso esta tiene prioridad.
*Parametrizable:* no (es una regla de cálculo del motor). *Decisión confirmada por el usuario.*

**BR-015 — Permanencias mayores a 24 horas**
Cuando la permanencia supera las 24 horas, el cobro se calcula como: (número de días completos de 24h × valor de la tarifa "todo el día" vigente para el tipo de vehículo) + (cobro por tiempo, aplicando fracción/redondeo, sobre el remanente inferior a 24h). Si no existe tarifa "todo el día" configurada para ese tipo de vehículo, se aplica la tarifa por tiempo sobre la totalidad de la permanencia.
*Parametrizable:* los valores de tarifa usados en el cálculo, sí; la fórmula en sí no. *Decisión confirmada por el usuario.*

**BR-016 — Capacidad máxima**
La capacidad máxima del parqueadero es un valor configurable en `Configuration`. Cuando el número de sesiones activas alcanza ese valor, el sistema **no bloquea** nuevos ingresos en el MVP; solo lo refleja visualmente en el dashboard (`Ocupados/Capacidad` y porcentaje de ocupación) como alerta informativa para el empleado/administrador.
*Parametrizable:* sí. *Decisión confirmada por el usuario.*

## Pagos y cierre de sesión

**BR-017 — Comprobante imprimible estándar**
El comprobante de pago se genera como vista imprimible/digital estándar (renderizable en navegador), no requiere integración con impresoras térmicas en el MVP.
*Parametrizable:* el formato/campos visibles, sí (según configuración del parqueadero). *Decisión confirmada por el usuario.*

**BR-018 — Medios de pago parametrizables**
El catálogo de medios de pago es configurable por el administrador. Valor semilla: `EFECTIVO`, `TRANSFERENCIA`, `OTROS`. No se almacena información bancaria sensible en ningún caso.
*Parametrizable:* sí. *Decisión confirmada por el usuario.*

**BR-019 — Offline solo de recursos estáticos**
La PWA garantiza disponibilidad offline únicamente del shell de la aplicación (recursos estáticos cacheados). Ninguna operación transaccional (ingreso, salida, cobro, caja) se ejecuta ni se encola sin conexión en el MVP; si no hay conexión al backend, la UI debe informarlo claramente y bloquear la acción transaccional.
*Parametrizable:* no. *Decisión confirmada por el usuario.*

**BR-020 — Cierre obligatorio con cobro**
No se puede marcar una sesión como `FINALIZADO` sin un pago registrado asociado, salvo la salida de un cliente mensual cubierto (BR-030, enmienda del 2026-10-19), o que un Administrador ejecute una anulación (`ANULADO`) explícita, la cual exige motivo obligatorio y queda registrada en auditoría con el usuario, fecha/hora, y estado anterior/nuevo.
*Parametrizable:* no. *Origen:* prompt maestro §8.

**BR-021 — Idempotencia de cobro**
Un mismo intento de cobro (mismo `id` de sesión, mismo request) no puede generar dos pagos. El backend debe usar una clave de idempotencia o control transaccional a nivel de base de datos para impedir doble cobro por doble clic o reintento de red.
*Parametrizable:* no. *Origen:* prompt maestro §23.

## Caja

**BR-022 — Caja única activa por turno/empleado**
Un empleado no puede abrir una nueva caja si ya tiene una caja abierta. Los pagos registrados durante el turno se asocian siempre a la caja activa de quien los registra.
*Parametrizable:* no. *Origen:* prompt maestro §12.

**BR-023 — Cierre de caja inmutable**
Una vez finalizado un cierre de caja, no puede modificarse salvo intervención administrativa expresa, la cual debe quedar registrada en auditoría (valor anterior y nuevo).
*Parametrizable:* no. *Origen:* prompt maestro §12.

**BR-023.1 — Algoritmo de arqueo (Fase 6, precisa BR-023)**
Al cerrar una caja:
- `efectivo_esperado = saldo_inicial + Σ(cobros con medio de pago de categoría CASH)`. Los cobros `ELECTRONIC`/`OTHER` **no** afectan el efectivo físico esperado, solo el `total_recaudado`.
- `total_recaudado = Σ(cobros CASH) + Σ(cobros ELECTRONIC) + Σ(cobros OTHER)`.
- `vehiculos_atendidos` = número de pagos registrados en la caja.
- `diferencia = efectivo_reportado − efectivo_esperado` (positiva = sobrante, negativa = faltante).
- Una vez cerrada (estado `CLOSED`), los totales calculados (`cashCollected`, `electronicCollected`, `otherCollected`, `totalCollected`, `vehiclesServed`, `expectedCash`) quedan congelados — no se recalculan. Una corrección administrativa (BR-023) solo puede modificar `efectivo_reportado` (y por tanto `diferencia`), nunca los totales ya calculados, porque estos reflejan transacciones que ya ocurrieron.
- Solo el empleado dueño de la caja (o un Administrador) puede cerrarla.
*Parametrizable:* no. *Decisión tomada en Fase 6 (2026-09-23), al no estar completamente especificado el algoritmo en la versión anterior de esta regla.*

## Auditoría

**BR-024 — No borrado físico de datos transaccionales**
Sesiones de parqueo, pagos y cierres de caja nunca se eliminan físicamente; los cambios de estado (`ANULADO`, corrección administrativa) se representan como nuevos estados o registros de auditoría, preservando el historial completo.
*Parametrizable:* no. *Origen:* prompt maestro §15.

**BR-025 — Auditoría obligatoria de operaciones sensibles**
Toda creación/edición de tarifas (el registro incluye todos los campos que afectan el cálculo —tipo de vehículo, modalidad, valor, **horario**, tolerancia, fracción, redondeo, prioridad, estado y vigencia— en el valor anterior y en el nuevo; hasta el 2026-09-29 solo se guardaban nombre, modalidad, valor, prioridad y estado; lo mismo aplica a la configuración del parqueadero, que hasta esa fecha se auditaba sin valores), cambio de la tarifa sugerida al cobrar (BR-008.1), anulación de sesión, corrección administrativa, reapertura de operación, cierre de caja y modificación de usuario genera un registro de auditoría con: usuario, acción, fecha, hora, entidad afectada, identificador, valor anterior (si aplica) y valor nuevo (si aplica).
*Parametrizable:* no. *Origen:* prompt maestro §15.

**BR-027 — Consecutivo del comprobante de ingreso**
Cada ingreso recibe al registrarse un número `ING-` + 6 dígitos, reservado de forma atómica en la misma transacción (contador `ENTRY` de `operation_counter`), único (índice `uk_parking_session_entry_number`) y nunca reutilizado, aunque la sesión se anule. Las sesiones creadas antes de la migración V6 no tienen número (se muestran con "—").
*Parametrizable:* no. *Origen:* solicitud del usuario (2026-10-05), RF-072.

## Mensualidades (2026-10-19)

**BR-028 — Estado de una mensualidad**
Con la fecha local del parqueadero (BR-026) y `paidUntil` (último día cubierto): `AL_DIA` si hoy ≤ `paidUntil`; `EN_GRACIA` si hoy ≤ `paidUntil` + días de gracia (Configuración, por defecto 3); `VENCIDA` después; `SIN_PAGO` si nunca pagó; `INACTIVA` si el administrador la desactivó.
*Parametrizable:* sí (días de gracia). *Origen:* solicitud del usuario.

**BR-029 — Reconocimiento al ingreso**
Si la placa normalizada (BR-002) es una placa autorizada de una mensualidad activa y esa mensualidad no tiene otro vehículo `EN_PARQUEADERO`, la sesión queda vinculada a la mensualidad. Si ya tiene uno dentro, el ingreso es de cliente normal y se informa al empleado. Una placa pertenece a una sola mensualidad.
*Parametrizable:* no. *Origen:* solicitud del usuario.

**BR-030 — Salida cubierta por mensualidad**
Una sesión vinculada cuya mensualidad está `AL_DIA` o `EN_GRACIA` se finaliza sin cobro, sin pago y sin exigir caja abierta (`calculatedAmount = 0`, sin tarifa); el cobro normal se rechaza (`SUBSCRIPTION_COVERED`). Si está `VENCIDA`, `SIN_PAGO` o `INACTIVA`, se cobra con las tarifas vigentes y la pantalla lo advierte.
*Parametrizable:* no. *Origen:* solicitud del usuario.

**BR-031 — Cobro mensual**
Se cobra en la caja abierta de quien cobra, con un medio de pago, por exactamente el valor pactado (`CHARGED_AMOUNT_MISMATCH` si no). Cubre desde el día siguiente a `paidUntil` (u hoy, si nunca pagó o ya venció) hasta la primera fecha con el día de corte posterior al inicio; no se prorratea. Usa el mismo consecutivo `OP-` que los cobros de salida, suma en el arqueo y en los reportes y deja un movimiento de caja.
*Parametrizable:* no. *Origen:* solicitud del usuario.

## Zona horaria y tiempo

**BR-026 — Servicio de tiempo centralizado**
Todas las fechas/horas del sistema (ingreso, salida, pagos, cierres, auditoría) se obtienen de un único servicio de tiempo. La zona horaria es configurable por parqueadero; valor semilla: `America/Bogota`.
*Parametrizable:* sí (zona horaria). *Origen:* prompt maestro §25.
