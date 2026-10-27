# ADR-012 — Otros ingresos y gastos de caja

**Estado:** Aceptado (2026-10-27), a pedido del usuario.

## Contexto

La caja solo registraba los cobros de parqueo y de mensualidades. El usuario pidió:
- registrar **gastos o salidas de caja**, solo el administrador;
- registrar **otros ingresos** por conceptos distintos al parqueo, cualquier usuario.

Decisiones del usuario:
- Los conceptos salen de un **catálogo** que administra el administrador, y cada movimiento lleva además una descripción libre.
- El gasto sale de una **caja abierta** que elige el administrador: la suya o la de un empleado.
- Se acepta **cualquier medio de pago activo**. Solo el efectivo afecta el efectivo esperado.
- Comprobante con OP-, anulación por el administrador, pestaña en Reportes e indicadores en el Dashboard.

## Decisión

- **Tablas nuevas** (migración V9):
  - `cash_concept`: tipo `INCOME`/`EXPENSE`, nombre único por tipo y estado activo.
  - `cash_transaction`: caja, tipo, concepto, descripción, valor, medio de pago, OP-, usuario y fecha, más los datos de la anulación (estado, motivo, quién la hizo y cuándo).
- **`cash_session`** guarda en el cierre dos columnas nuevas: `other_income_collected` y `expenses_total`.
- **No se reutiliza `cash_movement`**:
  - su `CHECK (type IN ('PAYMENT','ADJUSTMENT'))` no se puede ampliar en SQLite sin reconstruir la tabla;
  - ningún cálculo la lee, porque es un registro de solo escritura;
  - los nuevos movimientos necesitan concepto, medio de pago, OP- y anulación, columnas que ella no tiene.
- **Arqueo (BR-033):**
  - Los otros ingresos suman en el bucket de su medio de pago y en el total.
  - Los gastos van aparte: `expectedCash = saldo inicial + efectivo cobrado − gastos en efectivo`.
  - Un gasto en efectivo no puede superar el efectivo disponible.
- **Consecutivo (BR-034):** se comparte el contador `PAYMENT` (OP-) con los cobros. Así un número nunca se repite en caja.
- **Anulación (BR-035):**
  - Solo el administrador, con motivo, y solo mientras la caja está abierta. Así el cierre ya guardado no queda inconsistente.
  - Es lógica: el movimiento se marca `VOIDED`, no se borra.
- **Totales en el resto del sistema:**
  - Dashboard y reporte general: los otros ingresos suman en el recaudo; los gastos se muestran aparte, junto con el neto.
  - Reporte "Ingresos y gastos": usa el modelo tabular de ADR-008.
- **Respaldo (ADR-009):** incluye las dos tablas nuevas. El formato no cambia, porque un respaldo solo se importa en la misma versión de esquema.

## Consecuencias

- Los cierres de caja anteriores quedan con otros ingresos y gastos en 0.
- Para corregir un movimiento de una caja ya cerrada, la vía sigue siendo la corrección administrativa del cierre (BR-023).
