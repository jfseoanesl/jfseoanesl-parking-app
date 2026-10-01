# ADR-003 — Motor de tarifas: Strategy + Factory + Chain of Responsibility, sin `TariffRule` separado

**Estado:** Aceptado (2026-09-22)

## Contexto

El sistema debe calcular cobros según distintas modalidades de tarifa (AM, PM, todo el día, por tiempo), configurables por el administrador, sin codificar valores en el código fuente, y debe poder extenderse a futuras modalidades (nocturna, festiva, convenios) sin modificar las existentes (`prompt.md` §9-10).

## Problema

1. Qué patrón de diseño usar para desacoplar la selección y el cálculo de tarifa.
2. Si el modelo de dominio debe tener una entidad `TariffRule` separada de `Tariff` (como sugiere el modelo preliminar de `prompt.md` §24) o si los campos de condición pueden vivir directamente en `Tariff`.
3. Cómo resolver la prioridad cuando varias tarifas activas coinciden para la misma sesión.

## Alternativas consideradas

**Para el patrón de cálculo:**
- (a) Un único servicio con `switch` sobre la modalidad.
- (b) Strategy (una clase por modalidad) + Factory (selección) + Chain of Responsibility (resolución de prioridad ante empates/candidatas múltiples).

**Para el modelo de datos:**
- (a) `Tariff` y `TariffRule` como entidades separadas (1-a-N), donde `TariffRule` contendría las condiciones horarias/de fracción.
- (b) Todos los campos de configuración en `Tariff` directamente, sin tabla adicional.

## Decisión

- Patrón: **(b)** Strategy + Factory + Chain of Responsibility, según se detalla en `06-architecture.md`.
- Modelo de datos: **(b)** sin `TariffRule` separado en el MVP — los campos de condición viven en `Tariff` (ver `03-domain-model.md`).
- Prioridad: se ordena por el campo `priority` (1 = mayor prioridad); en empate, se calculan todas las candidatas y se aplica la de **menor cobro resultante** para el cliente (regla conservadora, BR-008), registrando en auditoría cuál se aplicó.

## Justificación

- El `switch` (alternativa a) viola abierto/cerrado: cada modalidad nueva obligaría a tocar código ya probado. Strategy+Factory permite agregar una modalidad como una clase nueva.
- Introducir `TariffRule` como tabla separada en el MVP es sobreingeniería (`prompt.md` §18/§41): ninguna tarifa del MVP necesita más de una condición horaria simultánea. La extensibilidad requerida se logra en el motor (nuevas `TariffStrategy`), no en el esquema.
- La regla de "menor cobro en empate" es la opción más segura funcionalmente (evita sobrecobrar al cliente por una ambigüedad de configuración del administrador) y es la recomendación por defecto que el usuario confirmó en `00-product-vision.md`.

## Consecuencias

- Si en el futuro se requiere una tarifa con **múltiples condiciones compuestas** (ej. "solo festivos Y solo en la noche"), se introducirá una entidad `TariffCondition` (o se reintroducirá `TariffRule`) como extensión del modelo — este ADR queda superado en ese momento por uno nuevo, no se modifica retroactivamente.
- El motor de tarifas concentra la complejidad de negocio más sensible del sistema; requiere la batería de pruebas de borde exigida por `prompt.md` §32 (AM/PM, cruce de medianoche, >24h, fracciones, tolerancia, prioridad/empates).
