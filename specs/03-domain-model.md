# 03 — Modelo de Dominio

> Fase 2 — SDD. Amplía y cierra el modelo preliminar de `prompt.md` §24. Cada entidad indica campos, invariantes (reglas de negocio que la afectan) e identificador. Todas las entidades usan **identificadores independientes del motor de base de datos** (UUID v4 almacenado como `CHAR(36)`/`VARCHAR(36)`), para no depender de auto-incrementales propios de SQLite o MySQL — requisito explícito de `prompt.md` §24 y clave para RNF-016.

## Decisión de diseño: `Tariff` sin `TariffRule` separado en el MVP

El modelo preliminar del prompt (§24) sugiere `Tariff` y `TariffRule` como entidades separadas. Tras análisis: en el MVP, cada tarifa tiene una única condición horaria y una única modalidad (AM, PM, todo el día, por tiempo) — no hay combinación de múltiples condiciones por tarifa. Introducir `TariffRule` como tabla aparte agregaría una relación 1-a-N sin beneficio funcional inmediato (sobreingeniería, contrario a `prompt.md` §18/§41).

**Decisión:** todos los campos de configuración (`hora_inicial`, `hora_final`, `tolerancia`, `tiempo_minimo`, `unidad_tiempo`, `fraccion`, `redondeo`, `prioridad`) viven directamente en `Tariff`. La extensibilidad para modalidades futuras (nocturna, festivos, convenios) se resuelve con el patrón Strategy/Factory del motor de tarifas (`06-architecture.md`), no con una tabla `TariffRule` adicional. Si en el futuro una tarifa necesita **múltiples condiciones compuestas** (ej. "festivo Y nocturno"), se introducirá `TariffCondition` como extensión — documentado como evolución futura, no como deuda técnica actual. Ver `decisions/ADR-003-motor-tarifario.md`.

---

## Entidades

### User

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| username | string | único, no nulo |
| passwordHash | string | no nulo (BCrypt, ver `10-security-specification.md`) |
| fullName | string | no nulo |
| roleId | UUID | FK → Role, no nulo |
| active | boolean | no nulo, default `true` |
| createdAt | timestamp | no nulo |
| updatedAt | timestamp | no nulo |

**Invariantes:** BR-003 (RBAC), BR-004 (hash de contraseña). Un usuario inactivo no puede autenticarse (RF-006).
**Índices:** único sobre `username`.

### Role

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| code | string | único (`ADMIN`, `EMPLOYEE`) |
| name | string | no nulo |

**Semilla:** `ADMIN`, `EMPLOYEE` (prompt.md §3). Tabla presente (no hardcodeado como enum) para permitir roles adicionales en el futuro sin migración de esquema, aunque la lógica de autorización del MVP solo reconoce estos dos códigos.

### VehicleType

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| code | string | único (`CAR`, `MOTORCYCLE`, extensible) |
| name | string | no nulo |
| active | boolean | no nulo, default `true` |

**Semilla:** `CAR` (Automóvil), `MOTORCYCLE` (Motocicleta) — RF-016. Nuevos tipos se agregan como filas (RF-017), sin cambios de código en el motor de cálculo (RNF-022).

### ParkingSession

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| plate | string | no nulo, normalizado (BR-002) |
| vehicleTypeId | UUID | FK → VehicleType, no nulo |
| entryDateTime | timestamp | no nulo, asignado por el servicio de tiempo (BR-026) |
| exitDateTime | timestamp | nulo hasta la salida |
| status | enum | `EN_PARQUEADERO` \| `FINALIZADO` \| `ANULADO`, no nulo |
| entryEmployeeId | UUID | FK → User, no nulo |
| exitEmployeeId | UUID | FK → User, nulo hasta la salida |
| appliedTariffId | UUID | FK → Tariff, nulo hasta el cálculo |
| calculatedAmount | decimal | nulo hasta el cálculo |
| notes | string | opcional |
| cancelReason | string | nulo salvo estado `ANULADO` |
| cancelledBy | UUID | FK → User, nulo salvo `ANULADO` |
| cancelledAt | timestamp | nulo salvo `ANULADO` |
| createdAt / updatedAt | timestamp | no nulos |

**Invariantes:** BR-001 (única sesión activa por placa), BR-020 (no finalizar sin pago salvo anulación), BR-024 (no borrado físico).
**Índices:** índice sobre `plate`; índice único parcial/condicional sobre `plate` **donde** `status = 'EN_PARQUEADERO'` (o equivalente aplicado a nivel de servicio con bloqueo transaccional si el motor de BD no soporta índice parcial — ver `07-data-model.md`).

### Tariff

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| name | string | no nulo |
| description | string | opcional |
| vehicleTypeId | UUID | FK → VehicleType, no nulo (BR-005) |
| modality | enum | `FULL_AM` \| `FULL_PM` \| `FULL_DAY` \| `BY_TIME`, no nulo |
| value | decimal | no nulo, ≥ 0 |
| firstFractionValue | decimal | opcional, ≥ 0, solo `BY_TIME`: valor de la primera fracción (BR-011); nulo = todas valen `value` |
| startTime | time | requerido para `FULL_AM`, `FULL_PM`, `FULL_DAY`; nulo para `BY_TIME` |
| endTime | time | ídem |
| tolerance | integer (minutos) | default 10 (BR-012) |
| minTime | integer (minutos) | opcional |
| timeUnit | enum | `MINUTES` \| `HOURS`, usado por `BY_TIME` |
| fraction | integer | tamaño de la fracción cobrable (BR-011) |
| rounding | enum | `UP` \| `DOWN` \| `NEAREST`, default `UP` |
| priority | integer | no nulo (BR-008, 1 = mayor prioridad) |
| active | boolean | no nulo, default `true` |
| validFrom | date | no nulo |
| validTo | date | opcional (nulo = indefinida) |
| createdAt / updatedAt | timestamp | no nulos |
| createdBy / updatedBy | UUID | FK → User |

**Invariantes:** BR-005, BR-006, BR-008, BR-009, BR-010 a BR-016. RF-021 (validación de consistencia de campos según modalidad).
**Auditoría:** toda creación/edición genera `AuditLog` (BR-025, RF-022).

### PaymentMethod

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| code | string | único (`CASH`, `TRANSFER`, `OTHER`, extensible) |
| name | string | no nulo |
| category | enum | `CASH` \| `ELECTRONIC` \| `OTHER`, no nulo (agregado en Fase 6 — ver nota) |
| active | boolean | no nulo, default `true` |

**Semilla:** Efectivo (`CASH`/`CASH`), Transferencia (`TRANSFER`/`ELECTRONIC`), Otros (`OTHER`/`OTHER`) (BR-018). Catálogo editable por el administrador (RF-014) sin cambios de código.

**Nota (Fase 6):** `category` se agregó vía `V3__payment_method_category.sql` porque el arqueo de caja (RF-055) necesita saber cuáles medios de pago afectan el efectivo físico esperado sin adivinar por el `code` (que es libre/parametrizable). Solo `category = CASH` suma al `expectedCash` de una caja.

### Payment

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| parkingSessionId | UUID | FK → ParkingSession, no nulo, único (una sesión tiene a lo sumo un pago — BR-021) |
| cashSessionId | UUID | FK → CashSession, no nulo |
| calculatedAmount | decimal | no nulo |
| chargedAmount | decimal | no nulo |
| paymentMethodId | UUID | FK → PaymentMethod, no nulo |
| operationNumber | string | único, generado por el sistema (consecutivo) |
| userId | UUID | FK → User (quien registró el cobro), no nulo |
| paidAt | timestamp | no nulo |
| createdAt | timestamp | no nulo |

**Invariantes:** BR-021 (idempotencia — unicidad de `parkingSessionId`), BR-017 (no se guarda información bancaria sensible: no hay campos de tarjeta/cuenta).
**Índices:** único sobre `parkingSessionId`; único sobre `operationNumber`.

### CashSession

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| employeeId | UUID | FK → User, no nulo |
| status | enum | `OPEN` \| `CLOSED`, no nulo |
| openedAt | timestamp | no nulo |
| initialBalance | decimal | no nulo |
| closedAt | timestamp | nulo hasta el cierre |
| cashCollected | decimal | calculado al cierre |
| electronicCollected | decimal | calculado al cierre |
| otherCollected | decimal | calculado al cierre |
| totalCollected | decimal | calculado al cierre |
| vehiclesServed | integer | calculado al cierre |
| expectedCash | decimal | `initialBalance + cashCollected` |
| reportedCash | decimal | ingresado por el empleado al cierre |
| difference | decimal | `reportedCash - expectedCash` |
| createdAt / updatedAt | timestamp | no nulos |

**Invariantes:** BR-022 (una caja abierta por empleado a la vez), BR-023 (cierre inmutable salvo corrección administrativa auditada).
**Índices:** índice único parcial sobre `employeeId` **donde** `status = 'OPEN'` (equivalente al de `ParkingSession`, mismo patrón de control de unicidad condicional).

### CashMovement

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| cashSessionId | UUID | FK → CashSession, no nulo |
| paymentId | UUID | FK → Payment, nulo (nulo = movimiento manual, ej. ajuste) |
| type | enum | `PAYMENT` \| `ADJUSTMENT`, no nulo |
| amount | decimal | no nulo |
| description | string | opcional, obligatoria si `type = ADJUSTMENT` |
| createdAt | timestamp | no nulo |

**Propósito:** ledger auditable de todo lo que afecta el efectivo/recaudo de una caja, más allá de los pagos automáticos (permite futuros ajustes manuales sin modificar `Payment`).

### Configuration

Entidad de **fila única** (singleton lógico) por instalación.

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK (una sola fila en MVP) |
| parkingName | string | no nulo |
| nit | string | opcional |
| address | string | opcional |
| phone | string | opcional |
| maxCapacity | integer | opcional (nulo = sin límite mostrado) |
| timezone | string | no nulo, default `America/Bogota` (BR-026) |
| currency | string | no nulo, default `COP` |
| receiptFormatJson | text/json | configuración de campos visibles del comprobante |
| updatedAt | timestamp | no nulo |
| updatedBy | UUID | FK → User |

### AuditLog

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User, no nulo |
| action | string | no nulo (ej. `TARIFF_UPDATED`, `SESSION_CANCELLED`, `CASH_CLOSED`) |
| entityName | string | no nulo (ej. `Tariff`, `ParkingSession`) |
| entityId | UUID | no nulo |
| oldValue | json/text | nulo si no aplica |
| newValue | json/text | nulo si no aplica |
| occurredAt | timestamp | no nulo |

**Invariante:** BR-025 (obligatorio para tarifas, anulaciones, correcciones, reaperturas, cierres de caja, usuarios). Solo append — nunca se edita ni se borra (BR-024).

---

### Subscription (RF-074, 2026-10-19)

| Campo | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| customerName | string | no nulo |
| documentId, phone, notes | string | nulos |
| monthlyFee | decimal | > 0 |
| cutOffDay | int | 1–28 |
| paidUntil | date | nulo = nunca pagó |
| active | boolean | no nulo |
| vehicles | SubscriptionVehicle[] | al menos una; `plate` única global |

**SubscriptionPayment**: `subscriptionId`, `cashSessionId`, `paymentMethodId`, `userId`, `amount`, `periodFrom`, `periodTo`, `operationNumber` (único, consecutivo OP compartido), `paidAt`. **ParkingSession.subscriptionId** (nulo = cliente normal). Invariantes: BR-028 a BR-031.

---

## Relaciones (resumen)

```
Role 1───N User
VehicleType 1───N ParkingSession
VehicleType 1───N Tariff
User 1───N ParkingSession   (entryEmployeeId, exitEmployeeId — dos relaciones)
Tariff 1───N ParkingSession (appliedTariffId, opcional)
ParkingSession 1───1 Payment
PaymentMethod 1───N Payment
CashSession 1───N Payment
CashSession 1───N CashMovement
Payment 1───1 CashMovement  (cuando type = PAYMENT)
User 1───N CashSession
User 1───N AuditLog
Configuration (singleton)
```

## Campos de auditoría transversales

Todas las entidades transaccionales (`ParkingSession`, `Tariff`, `Payment`, `CashSession`, `User`) incluyen `createdAt`/`updatedAt`; las que son editables administrativamente incluyen además `createdBy`/`updatedBy`. Ninguna tiene borrado físico — los estados (`ANULADO`, `active=false`) cumplen la función de soft delete (BR-024).
