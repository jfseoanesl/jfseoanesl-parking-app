-- BR-027 / RF-072: consecutivo del comprobante de ingreso (ING-000001), reservado de forma atomica
-- con el mismo contador que los pagos (V5__operation_counter, fila 'ENTRY'). Portable SQLite/MySQL.
-- Las sesiones anteriores quedan con entry_number NULL: no se asignan numeros retroactivos.

ALTER TABLE parking_session ADD COLUMN entry_number VARCHAR(20);

CREATE UNIQUE INDEX uk_parking_session_entry_number ON parking_session (entry_number);

INSERT INTO operation_counter (name, counter_value) VALUES ('ENTRY', 0);
