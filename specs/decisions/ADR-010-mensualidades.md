# ADR-010 — Mensualidades (clientes con pago mensual)

**Estado:** Aceptado (2026-10-19). Implementa RF-074 a RF-077 y BR-028 a BR-031, a pedido del usuario.

## Contexto

Algunos clientes pagan una **mensualidad**: usan el parqueadero a diario con un vehículo a la vez (no siempre el mismo) y pagan una vez al mes, en una fecha de corte. Hasta ahora, toda salida exigía un cobro con tarifa (BR-020).

Decisiones del usuario:
- Se usa una **lista de placas autorizadas**, reconocidas automáticamente al ingresar.
- El pago mensual se **cobra en caja**.
- Hay **días de gracia** configurables; vencidos, la salida se cobra como cliente normal.
- El **valor y el día de corte** se pactan con cada cliente.

## Decisión

- **Modelo propio** (`subscription`, `subscription_vehicle`, `subscription_payment`; migración V7):
  - Los pagos mensuales no se guardan en `payment`, porque esa tabla exige una sesión de parqueo (`parking_session_id` no nulo y único, BR-021). Volverla nullable obligaba a reconstruir la tabla en SQLite y debilitaba la idempotencia del cobro.
  - El pago mensual usa el **mismo contador `OP-`**, así que los consecutivos nunca se repiten entre ambos tipos de cobro.
- **Integración con la caja y los reportes:**
  - `CashSessionService.summarize` suma los pagos mensuales de la caja por categoría de medio de pago, así que afectan el efectivo esperado y el arqueo.
  - Cada cobro deja un `cash_movement` de tipo `PAYMENT` con descripción.
  - `ReportService` los incluye en `totalCollected` y en los desgloses por medio de pago y por empleado, y los informa también por separado.
- **Salida cubierta** (`exit-subscription`): la sesión queda `FINALIZADO` sin pago, sin tarifa y con valor 0, y no exige caja abierta. Es una enmienda explícita a BR-020.
  - La cotización de una sesión cubierta **no consulta el motor tarifario**: así no falla aunque no haya una tarifa aplicable.
  - `checkout` rechaza las sesiones cubiertas, para que no se cobren por error.
- **Estado** calculado, no almacenado (`SubscriptionPeriods.statusOf`), a partir de `paidUntil`, los días de gracia de la Configuración y la fecha local (BR-026). El periodo de cada pago se calcula en `SubscriptionPeriods`, sin dependencias, y está probado de forma unitaria.
- **Un vehículo a la vez:** el ingreso se vincula solo si la mensualidad no tiene otro vehículo `EN_PARQUEADERO`; si lo tiene, el ingreso es de cliente normal y se avisa.
- **Permisos:**
  - Crear, editar y activar mensualidades: solo el administrador, también restringido por URL en `SecurityConfig`, para devolver 403 antes de validar el cuerpo.
  - Consultar y cobrar: administrador y empleado.
  - Se auditan `SUBSCRIPTION_CREATED/UPDATED/ACTIVATED/DEACTIVATED`, con el valor anterior y el nuevo.
- **Respaldo (ADR-009):** las tres tablas nuevas se exportan e importan en orden de llaves foráneas.

## Alternativas descartadas

- **Pagos mensuales en la tabla `payment`**, con `parking_session_id` nullable: obligaba a reconstruir la tabla en SQLite y rompía la restricción única de BR-021.
- **Cobro de $0 en cada salida cubierta**: generaba pagos ficticios, consumía consecutivos `OP-` y exigía caja abierta para algo que no mueve dinero.
- **Prorratear el primer mes**: no lo pidió el usuario y complica el cálculo. El administrador puede pactar el valor.

## Consecuencias

- Las salidas cubiertas no generan comprobante de pago, porque no hay pago. Quedan en Historial como "Mensual" y en Reportes como "Salidas de clientes mensuales". El comprobante de ingreso (RF-072) sigue disponible.
- `vehiclesServed` de la caja cuenta solo los cobros de salida.
- Verificado:
  - `SubscriptionPeriodsTest`: cortes 5 y 28, febrero, pago adelantado, vencida y gracia.
  - `SubscriptionIntegrationTest`: permisos, placa duplicada, pago en caja con arqueo, reporte y comprobante, ingreso reconocido y segundo vehículo, salida cubierta y cobro normal rechazado, sin pago e inactiva.
  - Suite completa 106/106 en SQLite y MySQL 8.4, y E2E del flujo completo.
