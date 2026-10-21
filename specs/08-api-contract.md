# 08 — Contrato de API

> Fase 2 — SDD. Amplía los endpoints conceptuales de `prompt.md` §31 a un contrato completo para el MVP. El contrato formal en OpenAPI 3.0 vive en `specs/openapi.yaml`; este documento es su versión narrativa/tabular para lectura rápida y trazabilidad (`12-traceability-matrix.md`).

Convenciones: autenticación por cookie de sesión (`10-security-specification.md`); todas las respuestas de error usan el formato uniforme descrito ahí; fechas en ISO-8601 con offset; montos como número decimal en la unidad monetaria configurada (COP, sin decimales fraccionarios de centavo en la UI, pero `DECIMAL(12,2)` en el modelo).

## Autenticación

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | público | Autentica usuario/contraseña, crea sesión. |
| POST | `/api/auth/logout` | autenticado | Invalida la sesión activa. |
| GET | `/api/auth/me` | autenticado | Retorna el usuario y rol de la sesión activa. |

## Parqueo — ingreso, activos, salida

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/parking-sessions/active` | Admin, Empleado | Lista sesiones `EN_PARQUEADERO` (soporta `?plate=` para búsqueda). |
| POST | `/api/parking-sessions` | Empleado | Registra ingreso (RF-023). `409` si ya existe sesión activa para la placa (BR-001). |
| GET | `/api/parking-sessions/{id}` | Admin, Empleado | Detalle de una sesión (incluye `entryNumber`). |
| GET | `/api/parking-sessions/{id}/entry-receipt` | Admin, Empleado | Datos del comprobante de ingreso (RF-072): parqueadero, `entryNumber`, placa, tipo, fecha/hora, empleado, observaciones, estado. Cualquier estado (reimpresión). `404` si no existe. |
| GET | `/api/parking-sessions` | Admin | Historial con filtros `?from=&to=&plate=&status=&employeeId=` (RF-058). `from`/`to` obligatorios (fecha de ingreso); responde `{items, truncated}` con máximo 500 resultados. |
| POST | `/api/parking-sessions/{id}/quote` | Admin, Empleado | Calcula (sin registrar) el valor a cobrar si la sesión cerrara "ahora" — usado por la pantalla de salida antes de confirmar (UC-004 paso 3-4). Los campos de primer nivel son la tarifa sugerida; `options` lista todas las aplicables (sugerida primero, `suggested: true`) con su valor (BR-008.1). |
| POST | `/api/parking-sessions/{id}/checkout` | Empleado | Registra salida + cobro en una sola operación transaccional (RF-034 a RF-042). Body incluye `paymentMethodId`, `chargedAmount` y, opcional, `tariffId` (tarifa alterna elegida; omitido = la sugerida). `409` si ya está finalizada/anulada, `TARIFF_NOT_APPLICABLE` si la tarifa elegida no aplica a la sesión, o `CHARGED_AMOUNT_MISMATCH` si `chargedAmount` no es exactamente el valor calculado en ese momento (volver a cotizar). |
| POST | `/api/parking-sessions/{id}/cancel` | Admin | Anula la sesión (RF-068). Body requiere `reason`. |

## Tarifas

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/tariffs` | Admin | Lista tarifas (`?vehicleTypeId=&active=`). |
| POST | `/api/tariffs` | Admin | Crea tarifa (RF-018). `firstFractionValue` opcional (solo `BY_TIME`, ≥ 0; BR-011): valor de la primera fracción. |
| GET | `/api/tariffs/{id}` | Admin | Detalle. |
| PUT | `/api/tariffs/{id}` | Admin | Edita tarifa (RF-019), genera auditoría. |
| PATCH | `/api/tariffs/{id}/status` | Admin | Activa/desactiva (RF-020). |

## Pagos

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/payments/{id}/receipt` | Admin, Empleado | Datos del comprobante para una transacción (RF-050), incluidos dirección y teléfono del parqueadero. La vista imprimible (`/comprobantes/{id}`) aplica `receiptFormatJson` (RF-015). |
| GET | `/api/payment-methods` | Admin, Empleado | Catálogo de medios de pago activos. |
| POST | `/api/payment-methods` | Admin | Crea medio de pago (RF-014). |

> Nota: no existe `POST /api/payments` independiente — el pago siempre se crea junto con el checkout (`POST /api/parking-sessions/{id}/checkout`) para garantizar la invariante BR-020 (no cerrar sin cobrar) en una sola transacción atómica.

## Caja

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/cash-sessions/current` | Empleado | Caja abierta del usuario autenticado (o `404` si no tiene). |
| POST | `/api/cash-sessions/open` | Empleado | Abre caja (RF-052). `409` si ya tiene una abierta (BR-022). |
| GET | `/api/cash-sessions/{id}/summary` | Admin, Empleado | Totales en vivo antes del cierre (para mostrar previsualización). |
| POST | `/api/cash-sessions/{id}/close` | Empleado | Cierra caja con `reportedCash` (RF-055). |
| GET | `/api/cash-sessions` | Admin | Cajas cerradas por fecha de cierre (`?from=&to=`), base para la corrección de RF-057. |
| GET | `/api/cash-sessions/{id}/payments` | Admin, Empleado (dueño) | Cobros de la caja (RF-059); `403` si la caja es de otro empleado. |
| POST | `/api/cash-sessions/{id}/adjustments` | Admin | Corrección administrativa auditada sobre un cierre finalizado (BR-023). |

## Reportes y dashboard

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/dashboard` | Admin | Indicadores de RF-064/RF-065. |
| GET | `/api/reports/daily?date=` | Admin | Reporte diario. |
| GET | `/api/reports/summary?from=&to=` | Admin | Reporte semanal/mensual/personalizado (mismo endpoint parametrizado por rango). |
| GET | `/api/reports/subscriptions?from=&to=&status=AL_DIA,VENCIDA&q=` | Admin | Reporte de mensualidades (RF-078): `portfolio` (cartera con estado y `daysOverdue` a hoy, pagado en el período), `payments` (cobros del período), `collected`, `paymentsCount`, `activeCount`, `countByStatus`, `overdueAmount`. `status` acepta varios separados por coma (vacío = todos); `q` busca por cliente, documento o placa. `400` si un estado es inválido o `from > to`. |
| GET | `/api/reports/subscriptions/export?…&format=xlsx\|pdf\|csv` | Admin | Mismo reporte, con los mismos filtros, descargado como `reporte-mensualidades_<período>.<ext>`: Excel con hojas Resumen, Cartera y Pagos (números y autofiltro); PDF A4 apaisado; CSV por secciones. |
| GET | `/api/reports/summary/export?from=&to=&format=xlsx\|pdf\|csv` | Admin | Descarga el mismo reporte en Excel, PDF o CSV (RF-063, `decisions/ADR-008-exportacion-reportes.md`). `Content-Disposition: attachment; filename=reporte-parqueadero_<período>.<ext>`. CSV con `;`, UTF-8 con BOM y protección contra inyección de fórmulas. `400` si el formato no es `xlsx`/`pdf`/`csv` o si `from > to`. |

## Respaldo (RF-073, `decisions/ADR-009-respaldo-base-de-datos.md`)

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/admin/backup` | Admin | Descarga el respaldo completo (`parking-respaldo_<yyyy-MM-dd_HHmm>.json`): formato, versión de esquema, conteos y todas las tablas. Audita `DATABASE_EXPORTED`. |
| POST | `/api/admin/backup/import` | Admin | Cuerpo: el JSON del respaldo. **Reemplaza todos los datos** en una transacción. `400` si el formato no es válido, la versión de esquema no coincide, faltan tablas o no hay un administrador activo (los datos no cambian). Responde `{schemaVersion, counts}`. |

## Mensualidades (RF-074 a RF-077, `decisions/ADR-010-mensualidades.md`)

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/subscriptions` | Admin, Empleado | Lista con `status` (BR-028), `paidUntil`, placas y `vehicleInsidePlate`. |
| GET | `/api/subscriptions/{id}` | Admin, Empleado | Detalle. |
| POST | `/api/subscriptions` | Admin | Crea (`customerName`, `documentId`, `phone`, `monthlyFee`, `cutOffDay` 1–28, `notes`, `vehicles[{plate, vehicleTypeId}]`). `409 PLATE_IN_OTHER_SUBSCRIPTION`. |
| PUT | `/api/subscriptions/{id}` | Admin | Edita (reemplaza las placas). |
| PATCH | `/api/subscriptions/{id}/status` | Admin | `{active}`. |
| POST | `/api/subscriptions/{id}/payments` | Admin, Empleado | Cobro mensual en la caja abierta: `{paymentMethodId, amount}`; `409 NO_OPEN_CASH_SESSION`, `CHARGED_AMOUNT_MISMATCH`, `SUBSCRIPTION_INACTIVE`. Responde el pago con `periodFrom/periodTo` y `operationNumber`. |
| GET | `/api/subscriptions/{id}/payments` | Admin, Empleado | Historial de pagos. |
| GET | `/api/subscription-payments/{id}/receipt` | Admin, Empleado | Datos del comprobante de mensualidad. |
| POST | `/api/parking-sessions/{id}/exit-subscription` | Admin, Empleado | Salida sin cobro de un cliente mensual cubierto (BR-030); `409 SUBSCRIPTION_NOT_COVERING` si no lo está. |

Cambios relacionados: `POST /api/parking-sessions` devuelve `subscriptionId`, `subscriptionCustomerName` y `notice` (BR-029); `POST /quote` devuelve `subscription {id, customerName, status, paidUntil, covered}` y, si está cubierta, `options` vacías y valor 0; `POST /checkout` rechaza `SUBSCRIPTION_COVERED`; `GET /api/reports/summary` agrega `subscriptionCollected`, `subscriptionPayments` y `subscriberExits` (incluidas en `totalCollected` y en los desgloses por medio de pago y empleado); `GET /api/cash-sessions/{id}/payments` agrega `kind` (`PARKING`/`SUBSCRIPTION`); `GET/PUT /api/configuration` agrega `subscriptionGraceDays`. `GET /api/dashboard`: `collectedToday` incluye las mensualidades cobradas hoy (corrección del 2026-10-20).

## Usuarios

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/users` | Admin | Lista usuarios. |
| POST | `/api/users` | Admin | Crea usuario (RF-005). |
| PATCH | `/api/users/{id}/status` | Admin | Activa/desactiva (RF-006). |
| PUT | `/api/users/{id}/password` | Admin | Cambia contraseña de otro usuario (RF-007). |
| PUT | `/api/users/me/password` | Admin, Empleado | Cambia la propia contraseña (RF-008). |

## Configuración y catálogos

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/configuration` | Admin, Empleado (lectura limitada) | Configuración del parqueadero. |
| PUT | `/api/configuration` | Admin | Actualiza configuración (RF-010 a RF-015). |
| GET | `/api/vehicle-types` | Admin, Empleado | Lista tipos de vehículo activos. |
| POST | `/api/vehicle-types` | Admin | Crea tipo de vehículo (RF-017). |

## Auditoría y plataforma

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/audit-logs?entity=&entityId=&userId=&from=&to=` | Admin | Consulta auditoría (RF-067), filtrable por entidad, usuario y rango de fechas. |
| GET | `/api/health` | público | Health check (RF-071). |

## Ejemplo — flujo crítico `checkout`

Request:
```http
POST /api/parking-sessions/6a1f.../checkout
Content-Type: application/json

{
  "paymentMethodId": "cash-method-uuid",
  "chargedAmount": 5000
}
```

Response `200`:
```json
{
  "sessionId": "6a1f...",
  "plate": "ABC123",
  "entryDateTime": "2026-09-22T08:05:00-05:00",
  "exitDateTime": "2026-09-22T09:40:00-05:00",
  "appliedTariff": { "id": "...", "name": "Tarifa por tiempo - Auto", "modality": "BY_TIME" },
  "calculatedAmount": 5000,
  "chargedAmount": 5000,
  "paymentMethod": "CASH",
  "operationNumber": "OP-000123",
  "status": "FINALIZADO"
}
```

Response `409` (ya cerrada):
```json
{ "error": "CONFLICT", "message": "La sesión ya fue finalizada o anulada", "timestamp": "..." }
```

## Contrato formal

El archivo `specs/openapi.yaml` (OpenAPI 3.0) formaliza esquemas de request/response para los endpoints marcados como prioritarios del MVP (auth, parking-sessions, tariffs, cash-sessions, reports). Se ampliará endpoint por endpoint conforme cada módulo entra a implementación (Fases 5-8), como exige la trazabilidad de `12-traceability-matrix.md`.
