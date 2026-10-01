-- Consecutivo de operacion de los pagos (RF-049) reservado de forma atomica. Antes se calculaba
-- como COUNT(payment) + 1 en memoria: con dos cobros simultaneos en MySQL ambos obtenian el mismo
-- numero y el segundo fallaba por uk_payment_operation_number (500). Ver
-- PaymentRepositoryAdapter#nextOperationSequence. Portable (SQLite y MySQL). Ojo: "last_value"
-- es palabra reservada en MySQL 8 (funcion de ventana); por eso la columna es counter_value.

CREATE TABLE operation_counter (
    name       VARCHAR(40) NOT NULL PRIMARY KEY,
    counter_value BIGINT      NOT NULL
);

-- Arranca donde quedo el esquema anterior: los pagos nunca se borran (BR-024) y cada uno recibio
-- COUNT + 1, asi que COUNT(*) es exactamente el ultimo consecutivo emitido.
INSERT INTO operation_counter (name, counter_value)
SELECT 'PAYMENT', COUNT(*) FROM payment;
