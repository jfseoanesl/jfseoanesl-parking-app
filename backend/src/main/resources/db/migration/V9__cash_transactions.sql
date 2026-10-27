-- UC-014 / RF-079 a RF-084 / BR-032 a BR-035: otros ingresos y gastos de caja (ADR-012).
-- Tabla propia en lugar de cash_movement: su CHECK de tipos no se puede ampliar en SQLite sin reconstruir
-- la tabla y nadie la lee para los totales. Portable SQLite/MySQL: FK como restricciones de tabla (V3_1).

CREATE TABLE cash_concept (
    id     CHAR(36)     NOT NULL PRIMARY KEY,
    type   VARCHAR(10)  NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    name   VARCHAR(100) NOT NULL,
    active BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_cash_concept_type_name UNIQUE (type, name)
);

CREATE TABLE cash_transaction (
    id                CHAR(36)      NOT NULL PRIMARY KEY,
    cash_session_id   CHAR(36)      NOT NULL,
    type              VARCHAR(10)   NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    concept_id        CHAR(36)      NOT NULL,
    description       VARCHAR(255),
    amount            DECIMAL(12,2) NOT NULL,
    payment_method_id CHAR(36)      NOT NULL,
    -- BR-034: mismo consecutivo OP- que los cobros (contador PAYMENT de V5).
    operation_number  VARCHAR(30)   NOT NULL,
    user_id           CHAR(36)      NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    status            VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'VOIDED')),
    void_reason       VARCHAR(255),
    voided_by         CHAR(36),
    voided_at         DATETIME(6),
    CONSTRAINT uk_cash_transaction_operation_number UNIQUE (operation_number),
    CONSTRAINT fk_cash_transaction_cash_session FOREIGN KEY (cash_session_id) REFERENCES cash_session (id),
    CONSTRAINT fk_cash_transaction_concept FOREIGN KEY (concept_id) REFERENCES cash_concept (id),
    CONSTRAINT fk_cash_transaction_method FOREIGN KEY (payment_method_id) REFERENCES payment_method (id),
    CONSTRAINT fk_cash_transaction_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_cash_transaction_voided_by FOREIGN KEY (voided_by) REFERENCES app_user (id)
);

CREATE INDEX ix_cash_transaction_cash_session ON cash_transaction (cash_session_id);
CREATE INDEX ix_cash_transaction_created_at ON cash_transaction (created_at);

-- Foto del cierre (BR-033): otros ingresos y gastos de la caja. Las cajas anteriores quedan en 0.
ALTER TABLE cash_session ADD COLUMN other_income_collected DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE cash_session ADD COLUMN expenses_total DECIMAL(12,2) NOT NULL DEFAULT 0;

-- Conceptos iniciales editables por el administrador.
INSERT INTO cash_concept (id, type, name, active) VALUES
    ('00000000-0000-0000-0000-000000000501', 'INCOME', 'Otros ingresos', TRUE),
    ('00000000-0000-0000-0000-000000000502', 'EXPENSE', 'Gastos varios', TRUE);
