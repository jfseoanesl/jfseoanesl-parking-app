# 04 — Casos de Uso

> Fase 1 — SDD. Casos de uso principales derivados de `01-functional-requirements.md`. Cada uno incluye criterios de aceptación iniciales en formato Given/When/Then para los escenarios principales; se ampliarán con más detalle (flujos alternativos exhaustivos, validaciones de campo) en las especificaciones de feature (`/specs/features/*.md`) que se crearán al implementar cada módulo.

---

## UC-001 — Iniciar sesión

**Actor:** Administrador, Empleado
**Precondición:** El usuario existe y está activo.
**Flujo principal:**
1. El usuario ingresa usuario y contraseña.
2. El sistema valida credenciales.
3. El sistema inicia sesión y redirige a la pantalla principal según el rol.

**Flujos alternativos / excepciones:**
- Credenciales inválidas → mensaje de error genérico, sin indicar si el usuario existe.
- Usuario desactivado → mensaje de error, no se inicia sesión.

**Postcondición:** Sesión autenticada activa, con rol identificado para RBAC.

**Criterios de aceptación:**
```
Given un usuario activo con credenciales válidas
When envía usuario y contraseña correctos
Then el sistema inicia sesión y lo redirige a su pantalla principal según su rol

Given un usuario desactivado
When intenta iniciar sesión con credenciales correctas
Then el sistema rechaza el acceso sin iniciar sesión
```

**RF relacionados:** RF-001, RF-002, RF-004.

---

## UC-002 — Registrar ingreso de vehículo

**Actor:** Empleado
**Precondición:** Empleado autenticado; no existe sesión activa para la placa a registrar.
**Flujo principal:**
1. El empleado selecciona "Nuevo ingreso".
2. Escribe la placa.
3. Selecciona el tipo de vehículo.
4. Confirma.
5. El sistema normaliza la placa, asigna fecha/hora, registra el empleado y crea la sesión en estado `EN_PARQUEADERO`.
6. El sistema confirma visualmente el ingreso.

**Flujos alternativos / excepciones:**
- Ya existe sesión activa para esa placa → el sistema rechaza el ingreso e informa la sesión activa existente.
- Placa con formato no reconocible → el sistema advierte pero permite continuar si el empleado confirma explícitamente (evitar bloquear casos reales no previstos).

**Postcondición:** Nueva sesión de parqueo activa, visible en "Vehículos parqueados".

**Criterios de aceptación:**
```
Given que no existe un parqueo activo para la placa ABC123
When el empleado registra el ingreso con tipo "automóvil"
Then el sistema crea una sesión de parqueo activa con fecha/hora de ingreso asignadas por el sistema

Given que ya existe un parqueo activo para la placa ABC123
When un empleado intenta registrar un nuevo ingreso para la misma placa
Then el sistema rechaza la operación e informa que ya existe un parqueo activo
```

**RF relacionados:** RF-023 a RF-029. **BR:** BR-001, BR-002.

---

## UC-003 — Consultar vehículos actualmente estacionados

**Actor:** Administrador, Empleado
**Precondición:** Usuario autenticado.
**Flujo principal:**
1. El usuario abre "Vehículos parqueados".
2. El sistema lista las sesiones activas con placa, tipo, hora de ingreso, tiempo transcurrido y empleado.
3. El usuario busca/filtra por placa si lo requiere.

**Postcondición:** Ninguna (consulta).

**Criterios de aceptación:**
```
Given que existen 3 sesiones activas
When el usuario abre la pantalla de vehículos parqueados
Then el sistema muestra las 3 sesiones con placa, tipo, hora de ingreso y tiempo transcurrido

Given una lista de vehículos parqueados
When el usuario escribe parte de una placa en el buscador
Then la lista se filtra de forma incremental a las coincidencias
```

**RF relacionados:** RF-030 a RF-033.

---

## UC-004 — Registrar salida y cobro

**Actor:** Empleado
**Precondición:** Existe una sesión `EN_PARQUEADERO` para la placa buscada.
**Flujo principal:**
1. El empleado busca la placa y selecciona el parqueo activo.
2. El sistema asigna automáticamente fecha/hora de salida.
3. El sistema calcula el tiempo de permanencia y aplica la regla tarifaria correspondiente.
4. El sistema muestra el valor a cobrar y la sección "Tarifa": la sugerida y, si hay más de una aplicable, las alternas con su valor para elegir; si solo hay una, la muestra fija con la razón (BR-008.1).
5. (Opcional) El empleado elige una tarifa alterna; el sistema recalcula y muestra el nuevo valor.
6. El empleado selecciona el medio de pago.
7. El empleado confirma el cobro.
8. El sistema registra el pago con la tarifa elegida y cierra la sesión (`FINALIZADO`); si la tarifa difiere de la sugerida, registra el cambio en auditoría.

**Flujos alternativos / excepciones:**
- Intento de finalizar sin cobro → rechazado, salvo anulación administrativa (ver UC-011).
- Doble confirmación de cobro (doble clic) → el sistema procesa un único pago (idempotencia).
- Tarifa elegida no aplicable a la sesión (ej. franja horaria distinta) → rechazado con `TARIFF_NOT_APPLICABLE`; no se registra el cobro.
- El valor cambió entre la cotización y la confirmación (ej. se cumplió una nueva fracción) → rechazado con `CHARGED_AMOUNT_MISMATCH`; el sistema muestra el valor actualizado (conservando la tarifa elegida si sigue aplicando) y el empleado confirma de nuevo.

**Postcondición:** Sesión en estado `FINALIZADO`, pago registrado, comprobante disponible.

**Criterios de aceptación:**
```
Given una sesión activa para la placa ABC123 con 90 minutos de permanencia
When el empleado registra la salida y confirma el cobro con medio de pago "efectivo"
Then el sistema calcula el valor según la tarifa aplicable, registra el pago y cierra la sesión como FINALIZADO

Given una sesión activa
When el empleado intenta confirmar el cobro dos veces por doble clic
Then el sistema registra un único pago para esa sesión

Given una sesión activa con dos tarifas aplicables, "Por tiempo - Auto" (sugerida) y "Convenio"
When el empleado elige "Convenio" y confirma el cobro
Then el sistema cobra el valor calculado con "Convenio", la aplica a la sesión y registra en auditoría TARIFF_OVERRIDDEN con ambas tarifas y sus valores
```

**RF relacionados:** RF-034 a RF-042. **BR:** BR-008.1, BR-014 a BR-021.

---

## UC-005 — Calcular tarifa (usado internamente por UC-004)

**Actor:** Sistema (motor de tarifas), invocado por Empleado a través de UC-004.
**Precondición:** Sesión con fecha/hora de ingreso y salida definidas; existen tarifas activas para el tipo de vehículo.
**Flujo principal:**
1. El motor identifica las tarifas activas y vigentes para el tipo de vehículo de la sesión.
2. Resuelve cuál aplica según horario, prioridad y modalidad (BR-007, BR-008) y conserva el resto de aplicables, ordenadas, como alternas (BR-008.1).
3. Aplica tolerancia, fracción y redondeo configurados (BR-011, BR-012).
4. Retorna el valor calculado.

**Criterios de aceptación:**
```
Given una tarifa por tiempo con fracción de 60 minutos, tolerancia de 10 minutos y redondeo hacia arriba
When una sesión tiene 65 minutos de permanencia
Then el motor cobra 2 fracciones (una completa más una fracción iniciada tras la tolerancia)

Given dos tarifas activas que coinciden para el mismo tipo de vehículo y horario, con distinta prioridad
When el motor calcula el valor de una sesión
Then se aplica la tarifa con menor valor numérico de prioridad (BR-008)

Given una sesión con 30 horas de permanencia y tarifa "todo el día" configurada
When el motor calcula el valor
Then se cobra 1 día completo de tarifa "todo el día" más el cobro por tiempo del remanente de 6 horas (BR-015)
```

**RF relacionados:** RF-043 a RF-048. **BR:** BR-005 a BR-016.

---

## UC-006 — Abrir caja

**Actor:** Empleado
**Precondición:** El empleado no tiene una caja abierta actualmente.
**Flujo principal:**
1. El empleado selecciona "Abrir caja".
2. Ingresa el saldo inicial.
3. El sistema registra empleado, fecha, hora y saldo inicial, y activa la caja.

**Flujos alternativos / excepciones:**
- El empleado ya tiene una caja abierta → el sistema rechaza la apertura e indica la caja activa existente.

**Postcondición:** Caja activa asociada al empleado.

**Criterios de aceptación:**
```
Given que el empleado no tiene caja abierta
When abre caja con saldo inicial de $50.000
Then el sistema crea una caja activa asociada al empleado con ese saldo inicial

Given que el empleado ya tiene una caja abierta
When intenta abrir una nueva caja
Then el sistema rechaza la operación
```

**RF relacionados:** RF-052, RF-053. **BR:** BR-022.

---

## UC-007 — Cerrar caja

**Actor:** Empleado
**Precondición:** El empleado tiene una caja activa.
**Flujo principal:**
1. El empleado selecciona "Cerrar caja".
2. El sistema calcula y muestra: saldo inicial, cobros por medio de pago, total recaudado, vehículos atendidos, efectivo esperado.
3. El empleado ingresa el efectivo reportado (conteo físico).
4. El sistema calcula la diferencia y guarda el cierre como histórico.

**Postcondición:** Cierre de caja histórico e inmutable; caja pasa a inactiva.

**Criterios de aceptación:**
```
Given una caja activa con $200.000 en cobros de efectivo registrados y saldo inicial de $50.000
When el empleado cierra caja reportando $248.000 en efectivo
Then el sistema calcula un efectivo esperado de $250.000 y una diferencia de -$2.000, y guarda el cierre como histórico
```

**RF relacionados:** RF-055 a RF-057. **BR:** BR-023.

---

## UC-008 — Configurar / editar tarifa

**Actor:** Administrador
**Precondición:** Administrador autenticado.
**Flujo principal:**
1. El administrador abre "Tarifas" y selecciona crear o editar.
2. Ingresa/edita los campos de la tarifa (nombre, tipo de vehículo, modalidad, valor, horarios, tolerancia, fracción, redondeo, prioridad, vigencia, estado).
3. El sistema valida y guarda la tarifa.
4. El sistema registra el cambio en auditoría.

**Postcondición:** Tarifa creada/actualizada, disponible inmediatamente para el motor de cálculo (si está activa y vigente).

**Criterios de aceptación:**
```
Given un administrador autenticado
When crea una tarifa "todo el día" para motocicleta con valor $8.000 y estado activo
Then el sistema guarda la tarifa y queda disponible para el motor de cálculo en la siguiente sesión de moto que aplique

Given una tarifa existente
When el administrador la modifica
Then el sistema registra en auditoría el valor anterior y el nuevo
```

**RF relacionados:** RF-018 a RF-022. **BR:** BR-005, BR-006, BR-009, BR-025.

---

## UC-009 — Gestionar usuarios

**Actor:** Administrador
**Precondición:** Administrador autenticado.
**Flujo principal:**
1. El administrador abre "Usuarios".
2. Crea un usuario asignando rol, o edita/activa/desactiva uno existente, o cambia su contraseña.
3. El sistema guarda el cambio y lo registra en auditoría cuando corresponde.

**Criterios de aceptación:**
```
Given un administrador autenticado
When crea un usuario con rol "Empleado"
Then el nuevo usuario puede iniciar sesión y solo ve las funciones permitidas para su rol

Given un usuario activo
When el administrador lo desactiva
Then ese usuario no puede volver a iniciar sesión
```

**RF relacionados:** RF-005 a RF-009. **BR:** BR-003, BR-004, BR-025.

---

## UC-010 — Generar reporte

**Actor:** Administrador
**Precondición:** Administrador autenticado.
**Flujo principal:**
1. El administrador selecciona tipo de reporte (diario/semanal/mensual/personalizado) y, si aplica, el rango de fechas.
2. El sistema calcula y muestra los indicadores definidos en el prompt maestro §13.

**Criterios de aceptación:**
```
Given operaciones registradas en un día específico
When el administrador solicita el reporte diario de esa fecha
Then el sistema muestra número de vehículos ingresados/finalizados, total recaudado, recaudación por medio de pago y demás indicadores mínimos definidos
```

**RF relacionados:** RF-060 a RF-063.

---

## UC-011 — Anular sesión de parqueo (operación administrativa)

**Actor:** Administrador
**Precondición:** Existe una sesión que se desea anular (activa o finalizada según autorización).
**Flujo principal:**
1. El administrador selecciona la sesión y elige "Anular".
2. Ingresa un motivo obligatorio.
3. El sistema cambia el estado a `ANULADO`, sin borrar el registro, y guarda auditoría (usuario, motivo, fecha/hora, estado anterior/nuevo).

**Postcondición:** Sesión en estado `ANULADO`, visible en historial con trazabilidad completa.

**Criterios de aceptación:**
```
Given una sesión de parqueo activa sin cobro registrado
When el administrador la anula indicando un motivo
Then el sistema cambia el estado a ANULADO y registra la operación en auditoría, sin eliminar el registro
```

**RF relacionados:** RF-068. **BR:** BR-020, BR-024, BR-025.

---

## UC-012 — Consultar auditoría

**Actor:** Administrador
**Precondición:** Administrador autenticado.
**Flujo principal:**
1. El administrador abre "Auditoría".
2. Filtra por usuario, entidad o fecha.
3. El sistema muestra los registros de auditoría correspondientes.

**Criterios de aceptación:**
```
Given que existen registros de auditoría de cambios de tarifa
When el administrador filtra auditoría por entidad "Tariff" en un rango de fechas
Then el sistema muestra únicamente los registros de auditoría de tarifas en ese rango
```

**RF relacionados:** RF-066, RF-067.

---

## UC-013 — Configurar datos del parqueadero

**Actor:** Administrador
**Precondición:** Administrador autenticado.
**Flujo principal:**
1. El administrador abre "Configuración".
2. Edita nombre, NIT, dirección, teléfono, capacidad máxima, zona horaria, moneda, medios de pago, formato de comprobante.
3. El sistema guarda los cambios.

**Criterios de aceptación:**
```
Given un administrador autenticado
When configura la capacidad máxima en 40 cupos
Then el dashboard muestra "Ocupados/40" y el porcentaje de ocupación correspondiente
```

**RF relacionados:** RF-010 a RF-015. **BR:** BR-016, BR-018, BR-026.
